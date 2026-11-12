package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.FileItem
import com.example.ui.components.BatchActionBar
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FileItemRow
import com.example.ui.components.OperationProgressDialog
import com.example.ui.components.QuickSortRow
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.StorageInfoCard
import com.example.ui.components.TextEditorDialog
import com.example.ui.components.TextInputDialog
import com.example.ui.components.TopSearchBar
import com.example.ui.components.ZipViewerDialog
import com.example.util.FileManager
import com.example.util.SAFHelper
import com.example.util.SortOption
import com.example.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ClipboardState(
    val files: List<File>,
    val isCut: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val rootDir = remember { FileManager.getDefaultRootDirectory(context) }
    var currentDir by remember { mutableStateOf(rootDir) }

    // Seed sample files on initial load
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            FileManager.seedSampleDataIfEmpty(rootDir)
        }
    }

    var fileList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var showSortBottomSheet by remember { mutableStateOf(false) }

    // Multi-Selection State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    var clipboard by remember { mutableStateOf<ClipboardState?>(null) }

    // Dialog States
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemToViewZip by remember { mutableStateOf<File?>(null) }
    var itemToEditText by remember { mutableStateOf<File?>(null) }
    var itemDetails by remember { mutableStateOf<FileItem?>(null) }

    // Progress State
    var progressTitle by remember { mutableStateOf("") }
    var progressStep by remember { mutableStateOf("") }
    var progressPercent by remember { mutableIntStateOf(0) }
    var isOperating by remember { mutableStateOf(false) }

    fun refreshFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            val list = FileManager.listFiles(currentDir, searchQuery, sortOption)
            withContext(Dispatchers.Main) {
                fileList = list
            }
        }
    }

    LaunchedEffect(currentDir, searchQuery, sortOption) {
        refreshFiles()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedPaths = emptySet()
    }

    // Handle back button: Exit selection mode first, then navigate up directory
    val canGoBack = currentDir.absolutePath != rootDir.absolutePath && currentDir.parentFile != null
    BackHandler(enabled = isSelectionMode || canGoBack) {
        if (isSelectionMode) {
            exitSelectionMode()
        } else {
            val parent = currentDir.parentFile
            if (parent != null) {
                currentDir = parent
            }
        }
    }

    // SAF Tree Picker Launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            SAFHelper.savePersistedUri(context, uri)
            Toast.makeText(
                context,
                "Akses SAF berhasil diberikan: ${uri.lastPathSegment}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSelectionMode) "${selectedPaths.size} Dipilih" else "FolderMe",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = { exitSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Batal Pilih",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (canGoBack) {
                            IconButton(onClick = {
                                val parent = currentDir.parentFile
                                if (parent != null) currentDir = parent
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSelectionMode) {
                            // Toggle Selection Mode
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Pilih Banyak",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Sort Dialog
                            IconButton(onClick = { showSortBottomSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Pilihan Urutan",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Theme Toggle
                            IconButton(onClick = onToggleTheme) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Ganti Tema"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 1. Search Bar at Top (Filter realtime)
                TopSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Cari berkas atau folder di sini..."
                )

                // 2. Storage Overview Card Keren & Elegan
                StorageInfoCard()

                // 3. Storage Switcher Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentDir.absolutePath.startsWith(rootDir.absolutePath),
                        onClick = { currentDir = rootDir },
                        label = { Text("Penyimpanan FolderMe") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val ext = Environment.getExternalStorageDirectory()
                            if (ext != null && ext.canRead()) {
                                currentDir = ext
                            } else {
                                Toast.makeText(context, "Memerlukan izin penyimpanan", Toast.LENGTH_SHORT).show()
                            }
                        },
                        label = { Text("Memori Bersama") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            safLauncher.launch(null)
                        },
                        label = { Text("Pilih SD Card (SAF)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                // 4. Breadcrumb Keren dan Elegan (Clickable Hierarchy)
                BreadcrumbBar(
                    rootDir = rootDir,
                    currentDir = currentDir,
                    onNavigate = { targetDir ->
                        currentDir = targetDir
                    }
                )

                // 5. File Sort Row (Quick toggles & total count)
                QuickSortRow(
                    currentSort = sortOption,
                    onSortSelected = { newSort ->
                        sortOption = newSort
                    },
                    onOpenFullSortDialog = {
                        showSortBottomSheet = true
                    },
                    totalItems = fileList.size,
                    searchQuery = searchQuery
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                var fabMenuExpanded by remember { mutableStateOf(false) }
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = fabMenuExpanded) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    fabMenuExpanded = false
                                    showCreateFolderDialog = true
                                },
                                icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                text = { Text("Folder Baru") },
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    fabMenuExpanded = false
                                    showCreateFileDialog = true
                                },
                                icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                                text = { Text("File Teks Baru") },
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    fabMenuExpanded = false
                                    compressCurrentFolder(
                                        currentDir = currentDir,
                                        onStart = {
                                            isOperating = true
                                            progressTitle = "Mengompresi Folder ke ZIP..."
                                        },
                                        onProgress = { p, item ->
                                            progressPercent = p
                                            progressStep = item
                                        },
                                        onComplete = { success ->
                                            isOperating = false
                                            refreshFiles()
                                            Toast.makeText(
                                                context,
                                                if (success) "Folder berhasil dikompres ke ZIP!" else "Gagal mengompres",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                },
                                icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                text = { Text("Kompres Folder Ini ke ZIP") },
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Berkas/Folder",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Multi-Selection Batch Actions Bar
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BatchActionBar(
                    selectedCount = selectedPaths.size,
                    totalCount = fileList.size,
                    onSelectAll = {
                        selectedPaths = fileList.map { it.path }.toSet()
                    },
                    onDeselectAll = {
                        selectedPaths = emptySet()
                    },
                    onBatchDelete = {
                        showBatchDeleteConfirm = true
                    },
                    onBatchCopy = {
                        val targets = fileList.filter { selectedPaths.contains(it.path) }.map { it.file }
                        clipboard = ClipboardState(files = targets, isCut = false)
                        val count = targets.size
                        exitSelectionMode()
                        Toast.makeText(context, "$count item disalin ke papan klip", Toast.LENGTH_SHORT).show()
                    },
                    onBatchMove = {
                        val targets = fileList.filter { selectedPaths.contains(it.path) }.map { it.file }
                        clipboard = ClipboardState(files = targets, isCut = true)
                        val count = targets.size
                        exitSelectionMode()
                        Toast.makeText(context, "$count item siap dipindahkan", Toast.LENGTH_SHORT).show()
                    },
                    onBatchZip = {
                        val targets = fileList.filter { selectedPaths.contains(it.path) }.map { it.file }
                        exitSelectionMode()
                        isOperating = true
                        progressTitle = "Mengompresi ${targets.size} item ke ZIP..."
                        coroutineScope.launch(Dispatchers.IO) {
                            val targetZip = File(currentDir, "arsip_kumpulan_${System.currentTimeMillis() % 10000}.zip")
                            val ok = ZipUtils.createZip(targets, targetZip) { p, itm ->
                                progressPercent = p
                                progressStep = itm
                            }
                            withContext(Dispatchers.Main) {
                                isOperating = false
                                refreshFiles()
                                Toast.makeText(
                                    context,
                                    if (ok) "Arsip ${targetZip.name} berhasil dibuat!" else "Gagal mengompres",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onBatchShare = {
                        val targets = fileList.filter { selectedPaths.contains(it.path) && !it.isDirectory }.map { it.file }
                        exitSelectionMode()
                        if (targets.isEmpty()) {
                            Toast.makeText(context, "Pilih minimal 1 file (bukan folder) untuk dibagikan", Toast.LENGTH_SHORT).show()
                        } else {
                            shareMultipleFiles(context, targets)
                        }
                    },
                    onExitSelectionMode = {
                        exitSelectionMode()
                    }
                )
            }

            // Floating Clipboard Paste Bar
            AnimatedVisibility(visible = !isSelectionMode && clipboard != null) {
                clipboard?.let { clip ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (clip.files.size == 1) clip.files.first().name else "${clip.files.size} berkas",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = if (clip.isCut) "Siap dipindahkan ke sini" else "Siap disalin ke sini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = { clipboard = null }) {
                                Text("Batal")
                            }
                            TextButton(
                                onClick = {
                                    val sources = clip.files
                                    val isCut = clip.isCut
                                    clipboard = null

                                    isOperating = true
                                    progressTitle = if (isCut) "Memindahkan ${sources.size} item..." else "Menyalin ${sources.size} item..."
                                    coroutineScope.launch(Dispatchers.IO) {
                                        var allOk = true
                                        for (source in sources) {
                                            val ok = if (isCut) {
                                                FileManager.moveItem(source, currentDir) { p, itm ->
                                                    progressPercent = p
                                                    progressStep = itm
                                                }
                                            } else {
                                                FileManager.copyItem(source, currentDir) { p, itm ->
                                                    progressPercent = p
                                                    progressStep = itm
                                                }
                                            }
                                            if (!ok) allOk = false
                                        }
                                        withContext(Dispatchers.Main) {
                                            isOperating = false
                                            refreshFiles()
                                            Toast.makeText(
                                                context,
                                                if (allOk) "Operasi batch selesai!" else "Beberapa item gagal diproses",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Text("Tempel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (fileList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "Tidak ditemukan berkas '$searchQuery'"
                            } else {
                                "Direktori ini kosong"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "Coba kata kunci pencarian yang lain."
                            } else {
                                "Gunakan tombol (+) untuk membuat folder, file teks, atau kompresi ZIP."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                ) {
                    items(
                        items = fileList,
                        key = { it.path }
                    ) { item ->
                        val isSelected = selectedPaths.contains(item.path)

                        FileItemRow(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedPaths = if (isSelected) {
                                    selectedPaths - item.path
                                } else {
                                    selectedPaths + item.path
                                }
                            },
                            onClick = {
                                if (item.isDirectory) {
                                    searchQuery = ""
                                    currentDir = item.file
                                } else if (item.isZip) {
                                    itemToViewZip = item.file
                                } else if (item.extension in listOf("txt", "md", "log", "json", "xml", "csv")) {
                                    itemToEditText = item.file
                                } else {
                                    openFileWithIntent(context, item.file, item.mimeType)
                                }
                            },
                            onLongClick = {
                                // Long click starts multi-selection mode
                                isSelectionMode = true
                                selectedPaths = selectedPaths + item.path
                            },
                            onRename = {
                                itemToRename = item
                            },
                            onDelete = {
                                itemToDelete = item
                            },
                            onCopy = {
                                clipboard = ClipboardState(listOf(item.file), isCut = false)
                                Toast.makeText(context, "${item.name} disalin ke papan klip", Toast.LENGTH_SHORT).show()
                            },
                            onMove = {
                                clipboard = ClipboardState(listOf(item.file), isCut = true)
                                Toast.makeText(context, "${item.name} siap dipindahkan", Toast.LENGTH_SHORT).show()
                            },
                            onCompress = {
                                isOperating = true
                                progressTitle = "Mengompresi ke ZIP..."
                                coroutineScope.launch(Dispatchers.IO) {
                                    val targetZip = File(currentDir, "${item.file.nameWithoutExtension}_arsip.zip")
                                    val ok = ZipUtils.createZip(listOf(item.file), targetZip) { p, itm ->
                                        progressPercent = p
                                        progressStep = itm
                                    }
                                    withContext(Dispatchers.Main) {
                                        isOperating = false
                                        refreshFiles()
                                        Toast.makeText(
                                            context,
                                            if (ok) "Arsip ${targetZip.name} berhasil dibuat!" else "Gagal mengompres",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onExtract = {
                                extractZipArchive(
                                    zipFile = item.file,
                                    targetDir = currentDir,
                                    onStart = {
                                        isOperating = true
                                        progressTitle = "Mengekstrak ${item.name}..."
                                    },
                                    onProgress = { p, itm ->
                                        progressPercent = p
                                        progressStep = itm
                                    },
                                    onComplete = { ok ->
                                        isOperating = false
                                        refreshFiles()
                                        Toast.makeText(
                                            context,
                                            if (ok) "Ekstraksi ZIP selesai!" else "Gagal mengekstrak ZIP",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            },
                            onViewZip = {
                                itemToViewZip = item.file
                            },
                            onEditText = {
                                itemToEditText = item.file
                            },
                            onShare = {
                                shareFile(context, item.file, item.mimeType)
                            },
                            onDetails = {
                                itemDetails = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Batch Delete Confirmation Dialog
    if (showBatchDeleteConfirm) {
        val count = selectedPaths.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Hapus $count Berkas Terpilih?") },
            text = { Text("Semua $count berkas atau folder yang dipilih akan dihapus secara permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchDeleteConfirm = false
                        val targets = fileList.filter { selectedPaths.contains(it.path) }.map { it.file }
                        exitSelectionMode()
                        coroutineScope.launch(Dispatchers.IO) {
                            for (target in targets) {
                                FileManager.deleteItemRecursively(target)
                            }
                            withContext(Dispatchers.Main) {
                                refreshFiles()
                                Toast.makeText(context, "$count item berhasil dihapus", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialogs
    if (showCreateFolderDialog) {
        TextInputDialog(
            title = "Buat Folder Baru",
            label = "Nama Folder",
            confirmText = "Buat",
            onConfirm = { folderName ->
                showCreateFolderDialog = false
                val ok = FileManager.createFolder(currentDir, folderName)
                if (ok) refreshFiles() else Toast.makeText(context, "Nama folder sudah digunakan", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showCreateFileDialog) {
        TextInputDialog(
            title = "Buat File Teks Baru",
            label = "Nama File (contoh: catatan)",
            confirmText = "Buat",
            onConfirm = { fileName ->
                showCreateFileDialog = false
                val ok = FileManager.createTextFile(currentDir, fileName, "Dibuat dengan FolderMe pada ${java.util.Date()}\n\n")
                if (ok) refreshFiles() else Toast.makeText(context, "Nama file sudah digunakan", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCreateFileDialog = false }
        )
    }

    itemToRename?.let { item ->
        TextInputDialog(
            title = "Ganti Nama",
            initialValue = item.name,
            label = "Nama Baru",
            confirmText = "Ganti",
            onConfirm = { newName ->
                itemToRename = null
                val ok = FileManager.renameItem(item.file, newName)
                if (ok) refreshFiles() else Toast.makeText(context, "Gagal mengganti nama", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { itemToRename = null }
        )
    }

    itemToDelete?.let { item ->
        ConfirmDeleteDialog(
            itemName = item.name,
            isDirectory = item.isDirectory,
            onConfirm = {
                itemToDelete = null
                coroutineScope.launch(Dispatchers.IO) {
                    val ok = FileManager.deleteItemRecursively(item.file)
                    withContext(Dispatchers.Main) {
                        refreshFiles()
                        Toast.makeText(
                            context,
                            if (ok) "${item.name} berhasil dihapus" else "Gagal menghapus",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { itemToDelete = null }
        )
    }

    itemToViewZip?.let { zipFile ->
        ZipViewerDialog(
            zipFile = zipFile,
            onDismiss = { itemToViewZip = null },
            onExtract = { fileToExtract ->
                extractZipArchive(
                    zipFile = fileToExtract,
                    targetDir = currentDir,
                    onStart = {
                        isOperating = true
                        progressTitle = "Mengekstrak ${fileToExtract.name}..."
                    },
                    onProgress = { p, itm ->
                        progressPercent = p
                        progressStep = itm
                    },
                    onComplete = { ok ->
                        isOperating = false
                        refreshFiles()
                        Toast.makeText(
                            context,
                            if (ok) "Ekstraksi ZIP selesai!" else "Gagal mengekstrak ZIP",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        )
    }

    itemToEditText?.let { file ->
        TextEditorDialog(
            file = file,
            onDismiss = { itemToEditText = null },
            onSaveSuccess = {
                refreshFiles()
                Toast.makeText(context, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
            }
        )
    }

    itemDetails?.let { item ->
        FileDetailsDialog(item = item, onDismiss = { itemDetails = null })
    }

    if (showSortBottomSheet) {
        SortBottomSheet(
            currentSort = sortOption,
            onSelectSort = { selected ->
                sortOption = selected
            },
            onDismiss = { showSortBottomSheet = false }
        )
    }

    if (isOperating) {
        OperationProgressDialog(
            title = progressTitle,
            currentStep = progressStep,
            progressPercent = progressPercent
        )
    }
}

private fun compressCurrentFolder(
    currentDir: File,
    onStart: () -> Unit,
    onProgress: (Int, String) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    onStart()
    val children = currentDir.listFiles()?.toList() ?: emptyList()
    if (children.isEmpty()) {
        onComplete(false)
        return
    }
    val targetZip = File(currentDir, "${currentDir.name}_arsip.zip")
    Thread {
        val ok = ZipUtils.createZip(children, targetZip, onProgress)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onComplete(ok)
        }
    }.start()
}

private fun extractZipArchive(
    zipFile: File,
    targetDir: File,
    onStart: () -> Unit,
    onProgress: (Int, String) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    onStart()
    val extractSubfolder = File(targetDir, zipFile.nameWithoutExtension)
    Thread {
        val ok = ZipUtils.extractZip(zipFile, extractSubfolder, onProgress)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onComplete(ok)
        }
    }.start()
}

private fun openFileWithIntent(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membuka tipe berkas ini", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuka berkas: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan berkas via FolderMe"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan berkas: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMultipleFiles(context: Context, files: List<File>) {
    try {
        val uris = ArrayList<Uri>()
        for (f in files) {
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan ${files.size} berkas via FolderMe"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan berkas: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
