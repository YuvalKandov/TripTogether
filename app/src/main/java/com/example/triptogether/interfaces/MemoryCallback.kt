package com.example.triptogether.interfaces

import android.view.View
import com.example.triptogether.model.Memory

interface MemoryCallback {
    fun onMemoryClicked(memory: Memory, position: Int)
    fun onMemoryLongClicked(memory: Memory, position: Int, anchorView: View)
    fun onPhotoClicked(photoUrl: String)
    fun onMoreClicked(memory: Memory, position: Int, anchorView: View)
    fun onLikeClicked(memory: Memory, position: Int)
    fun onCommentClicked(memory: Memory, position: Int)
}
