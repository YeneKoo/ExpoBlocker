package expo.modules.appblockerengine.blocker.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper

class ButtonClickReceiver(
    private val context: Context,
    private val onButtonClicked: ((packageName: String) -> Unit)?
) : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == OverlayController.ACTION_BUTTON_CLICKED) {
            val packageName = intent.getStringExtra("packageName")
            if (packageName != null) {
                onButtonClicked?.invoke(packageName)
            }
        }
    }
    
    fun register() {
        val filter = IntentFilter(OverlayController.ACTION_BUTTON_CLICKED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(this, filter)
        }
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
    
    companion object {
        fun createAndRegister(context: Context, onButtonClicked: ((packageName: String) -> Unit)?): ButtonClickReceiver {
            val receiver = ButtonClickReceiver(context, onButtonClicked)
            receiver.register()
            return receiver
        }
    }
}
