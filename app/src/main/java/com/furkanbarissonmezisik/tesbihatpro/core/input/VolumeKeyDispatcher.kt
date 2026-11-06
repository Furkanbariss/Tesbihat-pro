package com.furkanbarissonmezisik.tesbihatpro.core.input

import android.view.KeyEvent

/** Ses tuşlarını kayıtlı dinleyiciye iletir; dinleyici yoksa olay sisteme kalır, ses normal değişir. */
class VolumeKeyDispatcher {

    interface Listener {
        fun onVolumeUp()
        fun onVolumeDown()
    }

    private var listener: Listener? = null

    fun register(listener: Listener) {
        this.listener = listener
    }

    fun unregister(listener: Listener) {
        if (this.listener === listener) this.listener = null
    }

    /**
     * Olay tüketildiyse true döner. Basılı tutmadaki tekrarlar sayılmaz ama tüketilir; tuşun
     * bırakılması da tüketilir, yoksa sistem sesi yine değiştirir.
     */
    fun onKeyEvent(keyCode: Int, action: Int, repeatCount: Int): Boolean {
        val current = listener ?: return false
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }
        if (action == KeyEvent.ACTION_DOWN && repeatCount == 0) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) current.onVolumeUp() else current.onVolumeDown()
        }
        return true
    }
}
