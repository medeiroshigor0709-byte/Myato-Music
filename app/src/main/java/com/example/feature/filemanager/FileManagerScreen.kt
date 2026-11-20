package com.example.feature.filemanager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeFormatter
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.storage.FileSystemNode
import com.example.feature.library.SimpleHeader
import java.io.File

/**
 * 16. Internal Music File Manager Screen — Linear & Hierarchical views, create folder,
 * rename, copy, move, delete with confirmation, multi-select, and library refresh.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    initialPath: String,
    onBack: () -> Unit,
    listDirectory: suspend (String) -> List<FileSystemNode>,
    listLinearAudio: suspend () -> List<FileSystemNode>,
    onCreateFolder: (String, String, () -> Unit) -> Unit,
    onRenameNode: (String, String, () -> Unit) -> Unit,
    onCopyFiles: (List<String>, String, () -> Unit) -> Unit,
    onMoveFiles: (List<String>, String, () -> Unit) -> Unit,
    onDeleteFiles: (List<String>, () -> Unit) -> Unit,
    onRefreshLibrary: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    var currentPath by remember { mutableStateOf(initialPath) }
    var isLinearView by remember { mutableStateOf(false) }
    var nodes by remember { mutableStateOf<List<FileSystemNode>>(emptyList()) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    val selectedPaths = remember { mutableStateListOf<String>() }
    var clipboardPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var clipboardIsMove by remember { mutableStateOf(false) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileSystemNode?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(currentPath, isLinearView, reloadNonce) {
        nodes = if (isLinearView) listLinearAudio() else listDirectory(currentPath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("file_manager_screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleHeader(title = "Gerenciador de Arquivos", onBack = onBack)
            }
            IconButton(onClick = { isLinearView = !isLinearView }) {
                Icon(
                    imageVector = if (isLinearView) Icons.Rounded.AccountTree else Icons.Rounded.ViewList,
                    contentDescription = "Alternar visualização linear / hierárquica",
                    tint = colors.primary,
                )
            }
            IconButton(onClick = { showCreateFolderDialog = true }) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Criar pasta", tint = colors.primary)
            }
            IconButton(onClick = { onRefreshLibrary(); reloadNonce++ }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar biblioteca", tint = colors.primary)
            }
        }

        if (!isLinearView) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.altBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    File(currentPath).parentFile?.let { parent ->
                        currentPath = parent.absolutePath
                        selectedPaths.clear()
                    }
                }) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir pasta", tint = colors.primary)
                }
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (selectedPaths.isNotEmpty() || clipboardPaths.isNotEmpty()) {
            Surface(color = colors.card, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedPaths.isNotEmpty()) {
                        Text("${selectedPaths.size} sel.", color = colors.primary, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = {
                            clipboardPaths = selectedPaths.toList()
                            clipboardIsMove = false
                            selectedPaths.clear()
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copiar", tint = colors.textPrimary)
                        }
                        IconButton(onClick = {
                            clipboardPaths = selectedPaths.toList()
                            clipboardIsMove = true
                            selectedPaths.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = "Mover", tint = colors.textPrimary)
                        }
                        if (selectedPaths.size == 1) {
                            val target = nodes.firstOrNull { it.path == selectedPaths.first() }
                            IconButton(onClick = { renameTarget = target }) {
                                Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = "Renomear", tint = colors.textPrimary)
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (clipboardPaths.isNotEmpty() && !isLinearView) {
                        TextButton(onClick = {
                            val items = clipboardPaths
                            clipboardPaths = emptyList()
                            if (clipboardIsMove) {
                                onMoveFiles(items, currentPath) { reloadNonce++ }
                            } else {
                                onCopyFiles(items, currentPath) { reloadNonce++ }
                            }
                        }) {
                            Text(
                                text = if (clipboardIsMove) "Mover aqui (${clipboardPaths.size})" else "Colar aqui (${clipboardPaths.size})",
                                color = colors.primary,
                            )
                        }
                    }
                }
            }
        }

        if (nodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Pasta vazia ou sem permissão de listagem direta.", color = colors.textSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                items(nodes, key = { it.path }) { node ->
                    val isSelected = node.path in selectedPaths
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) colors.primary.copy(alpha = 0.14f) else Color.Transparent)
                            .combinedClickable(
                                onClick = {
                                    if (selectedPaths.isNotEmpty()) {
                                        if (isSelected) selectedPaths.remove(node.path) else selectedPaths.add(node.path)
                                    } else if (node.isDirectory) {
                                        currentPath = node.path
                                    } else {
                                        selectedPaths.add(node.path)
                                    }
                                },
                                onLongClick = {
                                    if (isSelected) selectedPaths.remove(node.path) else selectedPaths.add(node.path)
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectedPaths.isNotEmpty()) {
                            Icon(
                                imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (isSelected) colors.primary else colors.textSecondary,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                        }
                        Icon(
                            imageVector = when {
                                node.isDirectory -> Icons.Rounded.Folder
                                node.isAudioFile -> Icons.Rounded.AudioFile
                                else -> Icons.Rounded.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = if (node.isDirectory || node.isAudioFile) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.name, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = if (node.isDirectory) "${node.childCount} itens" else TimeFormatter.formatFileSize(node.sizeBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = colors.card,
            title = { Text("Criar Nova Pasta", color = colors.textPrimary) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da pasta") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    onCreateFolder(currentPath, name) { reloadNonce++ }
                    showCreateFolderDialog = false
                }) { Text("Criar", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancelar") } },
        )
    }

    renameTarget?.let { node ->
        var newName by remember(node.path) { mutableStateOf(node.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = colors.card,
            title = { Text("Renomear", color = colors.textPrimary) },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    onRenameNode(node.path, newName) { selectedPaths.clear(); reloadNonce++ }
                    renameTarget = null
                }) { Text("Salvar", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancelar") } },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.card,
            title = { Text("Excluir permanentemente?", color = colors.textPrimary) },
            text = { Text("Deseja excluir ${selectedPaths.size} item(ns) selecionado(s)? Esta ação não pode ser desfeita.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = selectedPaths.toList()
                    selectedPaths.clear()
                    showDeleteConfirm = false
                    onDeleteFiles(toDelete) { reloadNonce++ }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } },
        )
    }
}
