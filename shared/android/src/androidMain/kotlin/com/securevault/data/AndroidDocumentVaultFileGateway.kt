package com.securevault.data

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.securevault.security.AndroidActivityProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AndroidDocumentVaultFileGateway : VaultFileGateway {

    override suspend fun pickExportTarget(suggestedFileName: String): Result<String> {
        return runCatching {
            val activity = requireActivity()
            val uri = launchCreateDocument(activity, suggestedFileName)
                ?: error("未选择导出文件")
            uri.toString()
        }
    }

    override suspend fun writeText(target: String, content: String): Result<Unit> {
        return runCatching {
            val activity = requireActivity()
            val uri = android.net.Uri.parse(target)
            activity.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(content.encodeToByteArray())
                output.flush()
            } ?: error("无法写入目标文件")
        }
    }

    override suspend fun pickImportSource(): Result<String> {
        return runCatching {
            val activity = requireActivity()
            val uri = launchOpenDocument(activity)
                ?: error("未选择导入文件")
            uri.toString()
        }
    }

    override suspend fun readText(source: String): Result<String> {
        return runCatching {
            val activity = requireActivity()
            val uri = android.net.Uri.parse(source)
            activity.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().decodeToString()
            } ?: error("无法读取导入文件")
        }
    }

    private fun requireActivity(): FragmentActivity {
        return AndroidActivityProvider.get() ?: error("当前没有可用的 Android Activity")
    }

    private suspend fun launchCreateDocument(
        activity: FragmentActivity,
        suggestedFileName: String,
    ): android.net.Uri? {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val key = "create_document_${System.nanoTime()}"
                val launcher = activity.activityResultRegistry.register(
                    key,
                    activity,
                    ActivityResultContracts.CreateDocument("application/json"),
                ) { uri ->
                    if (continuation.isActive) {
                        continuation.resume(uri)
                    }
                }

                continuation.invokeOnCancellation {
                    launcher.unregister()
                }

                launcher.launch(suggestedFileName)
            }
        }
    }

    private suspend fun launchOpenDocument(activity: FragmentActivity): android.net.Uri? {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val key = "open_document_${System.nanoTime()}"
                val launcher = activity.activityResultRegistry.register(
                    key,
                    activity,
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (continuation.isActive) {
                        continuation.resume(uri)
                    }
                }

                continuation.invokeOnCancellation {
                    launcher.unregister()
                }

                launcher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
            }
        }
    }
}
