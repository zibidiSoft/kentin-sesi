package io.github.thwisse.kentinsesi.util

import android.widget.ImageView
import coil.decode.SvgDecoder
import coil.load
import coil.request.ImageRequest
import io.github.thwisse.kentinsesi.R

/**
 * DiceBear API kullanarak avatar yükler
 * @param seed DiceBear seed string (UUID format)
 */
fun ImageView.loadAvatar(seed: String?) {
    android.util.Log.d("AvatarExtensions", "loadAvatar called - seed: '$seed'")
    
    if (seed.isNullOrBlank()) {
        // Fallback: Default placeholder icon
        android.util.Log.d("AvatarExtensions", "Seed is null/blank, showing placeholder")
        setImageResource(R.drawable.ic_person_placeholder)
        return
    }
    
    val url = "https://api.dicebear.com/9.x/personas/svg?seed=$seed&backgroundColor=transparent"
    android.util.Log.d("AvatarExtensions", "Loading avatar from URL: $url")
    
    load(url) {
        decoderFactory { result, options, _ ->
            SvgDecoder(result.source, options)
        }
        crossfade(true)
        placeholder(R.drawable.ic_person_placeholder)
        error(R.drawable.ic_person_placeholder)
        listener(
            onSuccess = { _, _ ->
                android.util.Log.d("AvatarExtensions", "✅ Avatar loaded successfully for seed: $seed")
            },
            onError = { _, result ->
                android.util.Log.e("AvatarExtensions", "❌ Avatar load FAILED for seed: $seed, error: ${result.throwable.message}")
            }
        )
    }
}
