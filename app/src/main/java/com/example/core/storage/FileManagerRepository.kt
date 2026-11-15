package com.example.core.storage

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.core.database.MyatoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileSystemNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val childCount: Int = 0,
    val isAudioFile: Boolean = false,
)

sealed interface FileOperationResult {
    data class Success(val message: String) : FileOperationResult
    data class Error(val message: String) : FileOperationResult
}

class FileManagerRepository(
    private val context: Context,
    private val database: MyatoDatabase,
) {
    private val audioExtensions = setOf(
        "mp3", "m4a", "wma", "flac", "opus", "aac", "alac", "ape", "dsf", "wav", "ogg"
    )

    fun defaultRootDirectory(): File {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir != null && musicDir.exists()) return musicDir
        return Environment.getExternalStorageDirectory() ?: context.filesDir
    }

    suspend fun listDirectory(path: String): List<FileSystemNode> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        val children = dir.listFiles()?.toList().orEmpty()
        children
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { file ->
                val ext = file.extension.lowercase()
                FileSystemNode(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    childCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0,
                    isAudioFile = file.isFile && ext in audioExtensions,
                )
            }
    }

    suspend fun listAllAudioFilesLinear(): List<FileSystemNode> = withContext(Dispatchers.IO) {
        database.trackDao().getAllTracksSync()
            .filter { !it.filePath.startsWith("content://") }
            .map { track ->
                val file = File(track.filePath)
                FileSystemNode(
                    name = file.name.ifBlank { "${track.title}.${track.formatExtension.lowercase()}" },
                    path = track.filePath,
                    isDirectory = false,
                    sizeBytes = track.sizeBytes,
                    lastModified = if (file.exists()) file.lastModified() else track.dateAddedSec * 1000L,
                    isAudioFile = true,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun createFolder(parentPath: String, folderName: String): FileOperationResult = withContext(Dispatchers.IO) {
        val cleanName = folderName.trim()
        if (cleanName.isBlank()) return@withContext FileOperationResult.Error("Nome da pasta inválido.")
        val target = File(parentPath, cleanName)
        if (target.exists()) return@withContext FileOperationResult.Error("Já existe uma pasta com esse nome.")
        return@withContext if (target.mkdirs()) {
            FileOperationResult.Success("Pasta \"$cleanName\" criada.")
        } else {
            FileOperationResult.Error("Sem permissão do sistema para criar pasta neste local. Use uma pasta acessível ou SAF.")
        }
    }

    suspend fun renameNode(path: String, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        val clean = newName.trim()
        if (clean.isBlank()) return@withContext FileOperationResult.Error("Novo nome inválido.")
        val src = File(path)
        if (!src.exists()) return@withContext FileOperationResult.Error("Arquivo ou pasta não encontrado.")
        val dst = File(src.parentFile, clean)
        if (dst.exists()) return@withContext FileOperationResult.Error("Já existe um item com esse nome.")
        return@withContext if (src.renameTo(dst)) {
            scanPath(dst.absolutePath)
            FileOperationResult.Success("Renomeado para \"$clean\".")
        } else {
            FileOperationResult.Error("O Android bloqueou a renomeação direta neste diretório (Scoped Storage).")
        }
    }

    suspend fun copyFiles(paths: List<String>, destinationDir: String): FileOperationResult = withContext(Dispatchers.IO) {
        val destFolder = File(destinationDir)
        if (!destFolder.exists() || !destFolder.isDirectory) {
            return@withContext FileOperationResult.Error("Pasta de destino inválida.")
        }
        var copied = 0
        for (srcPath in paths) {
            val src = File(srcPath)
            if (!src.exists() || src.isDirectory) continue
            val dst = uniqueDestination(destFolder, src.name)
            runCatching {
                src.copyTo(dst, overwrite = false)
                scanPath(dst.absolutePath)
                copied++
            }
        }
        if (copied > 0) {
            FileOperationResult.Success("$copied arquivo(s) copiado(s).")
        } else {
            FileOperationResult.Error("Não foi possível copiar os arquivos selecionados (verifique permissões do diretório).")
        }
    }

    suspend fun moveFiles(paths: List<String>, destinationDir: String): FileOperationResult = withContext(Dispatchers.IO) {
        val destFolder = File(destinationDir)
        if (!destFolder.exists() || !destFolder.isDirectory) {
            return@withContext FileOperationResult.Error("Pasta de destino inválida.")
        }
        var moved = 0
        for (srcPath in paths) {
            val src = File(srcPath)
            if (!src.exists() || src.isDirectory) continue
            val dst = uniqueDestination(destFolder, src.name)
            val renamed = runCatching { src.renameTo(dst) }.getOrDefault(false)
            if (renamed) {
                scanPath(dst.absolutePath)
                moved++
            } else {
                val copiedAndDeleted = runCatching {
                    src.copyTo(dst, overwrite = false)
                    if (src.delete()) {
                        scanPath(dst.absolutePath)
                        true
                    } else false
                }.getOrDefault(false)
                if (copiedAndDeleted) moved++
            }
        }
        if (moved > 0) {
            FileOperationResult.Success("$moved arquivo(s) movido(s).")
        } else {
            FileOperationResult.Error("Não foi possível mover os arquivos neste armazenamento (Scoped Storage).")
        }
    }

    suspend fun deleteFiles(paths: List<String>): FileOperationResult = withContext(Dispatchers.IO) {
        var deleted = 0
        for (path in paths) {
            val file = File(path)
            if (file.exists()) {
                val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
                if (ok) {
                    deleted++
                }
            }
        }
        if (deleted > 0) {
            FileOperationResult.Success("$deleted item(ns) excluído(s).")
        } else {
            FileOperationResult.Error("Não foi possível excluir: o Android exige confirmação do MediaStore ou acesso SAF neste diretório.")
        }
    }

    suspend fun deleteTrackFromLibraryOnly(trackId: Long) = withContext(Dispatchers.IO) {
        database.trackDao().deleteTrackById(trackId)
    }

    private fun uniqueDestination(dir: File, originalName: String): File {
        var candidate = File(dir, originalName)
        if (!candidate.exists()) return candidate
        val base = originalName.substringBeforeLast('.')
        val ext = originalName.substringAfterLast('.', "")
        var idx = 1
        while (candidate.exists()) {
            val numbered = if (ext.isNotEmpty()) "${base}_$idx.$ext" else "${base}_$idx"
            candidate = File(dir, numbered)
            idx++
        }
        return candidate
    }

    private fun scanPath(path: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }
}
