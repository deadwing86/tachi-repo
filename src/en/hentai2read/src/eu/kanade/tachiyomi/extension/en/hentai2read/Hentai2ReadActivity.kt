package eu.kanade.tachiyomi.extension.en.hentai2read

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

/**
 * Springboard that accepts https://hentai2read.com/xxxx intents
 * and redirects them to the main Tachiyomi process.
 */
class Hentai2ReadActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainIntent = Intent().apply {
            action = "eu.kanade.tachiyomi.SEARCH"
            putExtra("query", intent.data.toString())
            putExtra("filter", packageName)
        }

        try {
            startActivity(mainIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e("Hentai2ReadUrlActivity", "Unable to launch activity", e)
        }

        finish()
        exitProcess(0)
    }
}
