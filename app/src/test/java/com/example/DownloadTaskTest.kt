package com.example

import org.junit.Test
import java.io.File
import java.net.URL

class DownloadTaskTest {
    @Test
    fun downloadFonts() {
        val fontDir = File("src/main/res/font")
        fontDir.mkdirs()
        
        val urls = mapOf(
            "hind_siliguri_regular.ttf" to "https://raw.githubusercontent.com/google/fonts/main/ofl/hindsiliguri/HindSiliguri-Regular.ttf",
            "hind_siliguri_medium.ttf" to "https://raw.githubusercontent.com/google/fonts/main/ofl/hindsiliguri/HindSiliguri-Medium.ttf",
            "hind_siliguri_semibold.ttf" to "https://raw.githubusercontent.com/google/fonts/main/ofl/hindsiliguri/HindSiliguri-SemiBold.ttf",
            "hind_siliguri_bold.ttf" to "https://raw.githubusercontent.com/google/fonts/main/ofl/hindsiliguri/HindSiliguri-Bold.ttf"
        )
        
        urls.forEach { (name, urlStr) ->
            val url = URL(urlStr)
            val bytes = url.readBytes()
            File(fontDir, name).writeBytes(bytes)
        }
    }
}
