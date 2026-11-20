package app.steptwelve.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun MainViewModel.refreshPermissions(context: Context) = updatePermissions(
    contacts = context.hasPermission(Manifest.permission.READ_CONTACTS),
    callLog = context.hasPermission(Manifest.permission.READ_CALL_LOG),
)
