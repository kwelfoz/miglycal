package app.miglycal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import app.miglycal.ui.MiglycalApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var hasPerm by mutableStateOf(false)

    private val askPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { checkPerm() }

    private val audioPerm: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = dark, navigationBarStyle = dark)
        vm.player.connect()
        checkPerm()
        setContent { MiglycalApp(vm, hasPerm) { request() } }
    }

    override fun onResume() {
        super.onResume()
        checkPerm()
    }

    private fun checkPerm() {
        val ok = ContextCompat.checkSelfPermission(this, audioPerm) == PackageManager.PERMISSION_GRANTED
        if (ok && !hasPerm) vm.refresh()
        hasPerm = ok
    }

    private fun request() {
        val list = mutableListOf(audioPerm)
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT < 30) list += Manifest.permission.WRITE_EXTERNAL_STORAGE
        askPerms.launch(list.toTypedArray())
    }
}
