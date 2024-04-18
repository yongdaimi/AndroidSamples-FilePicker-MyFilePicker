package com.zbycorp.wx.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.zbycorp.filepicker.ZbyFilePicker
import com.zbycorp.filepicker.ui.FilePickerActivity
import com.zbycorp.wx.R
import com.zbycorp.wx.utils.*
import java.lang.StringBuilder
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    private val permissionRequestCode = 100

    private val fileRequestCode = 200

    private val excelPattern = Pattern.compile(".+.xlsx")

    private lateinit var btn_import:Button
    private lateinit var btn_send:Button
    private lateinit var pb_main: ProgressBar
    private lateinit var tv_content: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUiView()

        btn_import.setOnClickListener {
            if (PermissionChecker.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    permissionRequestCode
                )
            } else {
                importExcel()
            }

        }

        btn_send.setOnClickListener {
            if (isAccessibilitySettingsOn(this@MainActivity)) {
                WeChatAccessUtil.openWeChat(this)
            } else {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("请在无障碍服务中给该应用授权，否则无法使用该软件")
                    .setPositiveButton("设置") { _, _ ->
                        val accessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(accessibleIntent)
                    }
                    .setNegativeButton("取消") { _, _ -> onBackPressed() }
                    .show()
            }
        }
    }

    private fun initUiView() {
        btn_import = findViewById(R.id.btn_import)
        btn_send = findViewById(R.id.btn_send)
        pb_main = findViewById(R.id.pb_main)
        tv_content = findViewById(R.id.tv_content)
    }


    private fun importExcel() {
        ZbyFilePicker()
            .withActivity(this)
            .withRequestCode(fileRequestCode)
            .withHiddenFiles(false)
            .withTitle("子不语文件选择器")
            .withFilter(excelPattern)
            .start()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                importExcel()
            } else {
                toast("拒绝了权限")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileRequestCode && resultCode == Activity.RESULT_OK) {
            val path = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)
            if (!path.isEmpty()) {
                pb_main.visibility = View.VISIBLE
                async {
                    val datas = FileUtils.parseExcel(path!!)
                    sync {
                        pb_main.visibility = View.GONE
                    }

                    val sb = StringBuilder()
                    datas.forEach {
                        sb.append(it.first)
                            .append("：")
                            .append(it.second)
                            .append("\n")
                    }
                    tv_content.text = sb.toString()
                    WeChatAccessUtil.nameContentList = datas
                }
            }
        }
    }

    private fun isAccessibilitySettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
            Log.i("URL", "错误信息为：" + e.message)
        }

        if (accessibilityEnabled == 1) {
            val services =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (services != null) {
                return services.toLowerCase().contains(context.packageName.toLowerCase())
            }
        }
        return false
    }

}
