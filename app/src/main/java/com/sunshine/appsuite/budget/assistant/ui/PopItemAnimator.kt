package com.sunshine.appsuite.budget.assistant.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class PopItemAnimator : DefaultItemAnimator() {

    init {
        addDuration = 180
        removeDuration = 120
        moveDuration = 120
        changeDuration = 120
        supportsChangeAnimations = false
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        val v = holder.itemView

        // estado inicial
        v.alpha = 0f
        v.translationY = 18f
        v.scaleX = 0.96f
        v.scaleY = 0.96f

        dispatchAddStarting(holder)

        v.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(addDuration)
            .setInterpolator(OvershootInterpolator(1.15f))
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.animate().setListener(null)
                    dispatchAddFinished(holder)
                }
            })
            .start()

        return true
    }
}
