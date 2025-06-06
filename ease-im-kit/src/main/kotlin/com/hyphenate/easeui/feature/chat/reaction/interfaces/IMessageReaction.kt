package com.hyphenate.easeui.feature.chat.reaction.interfaces

import android.view.View
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.viewmodel.reaction.IChatReactionRequest

interface IMessageReaction {

    /**
     * Setup with message.
     */
    fun setupWithMessage(message: ChatMessage)

    /**
     * Set custom ViewModel.
     */
    fun setViewModel(viewModel: IChatReactionRequest?)

    /**
     * Show reactions.
     */
    fun showReaction()

    /**
     * Add a reaction to the message.
     */
    fun addReaction(reaction: String)

    /**
     * Remove a reaction from the message.
     */
    fun removeReaction(reaction: String)

    /**
     * Show more reactions.
     */
    fun showMoreReactions(view: View?) {}

    /**
     * Set more reaction click listener.
     */
    fun setMoreReactionClickListener(listener: View.OnClickListener?) {}

    /**
     * Set reaction error listener.
     */
    fun setReactionErrorListener(listener: OnChatUIKitReactionErrorListener?)
}