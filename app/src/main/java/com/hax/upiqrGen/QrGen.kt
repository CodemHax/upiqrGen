package com.hax.upiqrGen
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder


fun generateQrCode(text: String, size: Int): Bitmap?{
    return try {
        val bitMatrix : BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size,size)
        val barcodeEncoder = BarcodeEncoder()
        barcodeEncoder.createBitmap(bitMatrix)
    } catch (e : Exception){
        e.printStackTrace()
        null
    }

}

