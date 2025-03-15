package com.hax.upiqrGen

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.hax.upiqrGen.ui.theme.UpiqrGenTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    var upiId by mutableStateOf("")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadSavedData()

        setContent {
            UpiqrGenTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    QRDisplay(
                        upiId = upiId,
                        onUpiIdChange = { upiId = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun saveData() {
        val savePref = getPreferences(MODE_PRIVATE)
        with(savePref.edit()) {
            putString("upiId", upiId)
            apply()
        }
    }

    private fun loadSavedData() {
        val sharedPref = getPreferences(MODE_PRIVATE)
        upiId = sharedPref.getString("upiId", "").toString()
    }

    override fun onResume() {
        super.onResume()
        loadSavedData()
    }

    override fun onRestart() {
        super.onRestart()
        loadSavedData()
    }

    override fun onPause() {
        super.onPause()
        saveData()
    }

    override fun onStop() {
        super.onStop()
        saveData()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveData()
    }
}

@Composable
fun QRDisplay(
    upiId: String,
    onUpiIdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tempUpiId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showRealQr by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEmptyUpiMessage by remember { mutableStateOf(false) }
    var upiIdError by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(upiId) {
        upiIdError = if (upiId.isNotEmpty() &&
            (!upiId.contains("@") || upiId.split("@").size != 2)) {
            "Invalid UPI ID format (should be username@bank)"
        } else {
            null
        }
    }

    LaunchedEffect(key1 = Unit) {
        if (upiId.isEmpty()) {
            showEmptyUpiMessage = true
        }
    }

    fun shareQRCode(bitmap: Bitmap) {
        try {

            val imagesDir = File(context.cacheDir, "shared_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val file = File(imagesDir, "upi_qr_code.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()


            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share UPI QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share QR code", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upi Qr Generator",
                fontSize = 24.sp,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
            )
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Black
                )
            }
        }

        OutlinedTextField(
            value = upiId,
            onValueChange = { onUpiIdChange(it) },
            label = { Text("UPI ID") },
            isError = upiIdError != null,
            modifier = Modifier.fillMaxWidth()
        )

        upiIdError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (upiId.isNotBlank() && amount.isNotBlank() && upiIdError == null) {
                    showRealQr = true
                    val url = "upi://pay?pn=User&am=$amount&cu=INR&pa=$upiId"
                    qrBitmap = generateQrCode(url, 700)
                    Toast.makeText(context, "QR Code generated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please enter a valid UPI ID and amount", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate QR Code")
        }

        if (showRealQr) {
            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { shareQRCode(bitmap) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Share QR Code")
                }
            }
        } else {
            val url = "upi://pay?mode=01&pa=$upiId"
            val bitmap = generateQrCode(url, 700)
            qrBitmap = bitmap
            bitmap?.asImageBitmap()?.let {
                Image(
                    bitmap = it,
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { shareQRCode(bitmap) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Share QR Code")
                }
            }
        }
    }

    if (showEmptyUpiMessage) {
        AlertDialog(
            onDismissRequest = { showEmptyUpiMessage = false },
            title = { Text("UPI ID Required") },
            text = { Text("Please set default UPI ID using the settings button") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyUpiMessage = false
                        showSettingsDialog = true
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showEmptyUpiMessage = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Settings") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Default Upi Id", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = tempUpiId,
                        onValueChange = { tempUpiId = it },
                        label = { Text("Default Upi Id") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newUpi = tempUpiId.toString()
                        if (newUpi.isEmpty() || (newUpi.contains("@") && newUpi.split("@").size == 2)) {
                            onUpiIdChange(newUpi)
                            showSettingsDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a valid UPI ID", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    UpiqrGenTheme {
        QRDisplay(
            upiId = "",
            onUpiIdChange = {},
            modifier = Modifier
        )
    }
}