package com.hyphenate.easeui.feature.group.adapter

import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import com.hyphenate.EMValueCallBack
import com.hyphenate.chat.EMGroup
import com.hyphenate.easeui.base.ChatUIKitBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.extensions.toProfile
import com.hyphenate.easeui.databinding.UikitLayoutContactItemBinding
import com.hyphenate.easeui.model.ChatUIKitProfile
import com.hyphenate.easeui.model.ChatUIKitUser

class ChatUIKitGroupMemberListAdapter(
    private val groupId: String?
): ChatUIKitBaseRecyclerViewAdapter<ChatUIKitUser>() {

    private var isShowInitLetter:Boolean = true
    override fun getViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<ChatUIKitUser> =
        GroupMemberListViewHolder(
            UikitLayoutContactItemBinding.inflate(LayoutInflater.from(parent.context))
        )

    override fun onBindViewHolder(holder: ViewHolder<ChatUIKitUser>, position: Int) {
        if (holder is GroupMemberListViewHolder){
            holder.setShowInitialLetter(isShowInitLetter)
        }
        super.onBindViewHolder(holder, position)
    }


    fun setShowInitialLetter(isShow:Boolean){
        this.isShowInitLetter = isShow
    }

    inner class GroupMemberListViewHolder(
        private val mViewBinding: UikitLayoutContactItemBinding
    ) : ViewHolder<ChatUIKitUser>(binding = mViewBinding) {
        private var bgDrawable: Drawable? = null
        private var isShowInitLetter:Boolean = true
        private var user:ChatUIKitUser?=null

        override fun initView(viewBinding: ViewBinding?) {
            super.initView(viewBinding)
            viewBinding?.let {
                if (it is UikitLayoutContactItemBinding) {
                    bgDrawable = it.root.background
                }
            }
        }

        fun setShowInitialLetter(isShow:Boolean){
            this.isShowInitLetter = isShow
        }

        override fun setData(item: ChatUIKitUser?, position: Int) {
            this.user = item
            mViewBinding.let {
                val header = item?.initialLetter
                it.header.visibility = View.GONE
                it.emPresence.setUserAvatarData(user?.toProfile())
                it.tvName.text = user?.nickname ?: user?.userId

                it.switchMute.visibility = View.GONE

                groupId?.let { id ->
                    ChatUIKitProfile.getGroupMember(id, user?.userId)?.let { profile ->
                        it.emPresence.setUserAvatarData(profile)
                        it.tvName.text = profile.getRemarkOrName()

                        if (isShowMute) {
                            it.switchMute.visibility = View.VISIBLE
                            val userId = profile.id

                            Log.d("groupMute", "loadData: ${muteUserIdList.contains(userId)}  ${muteUserIdList}")
                            val isMuted = muteUserIdList.contains(userId)
                            it.switchMute.setOnCheckedChangeListener(null) // 先清除旧监听器避免触发
                            it.switchMute.isChecked = isMuted

                            it.switchMute.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    muteUserIdList.add(userId)
//                                    // ✅ 设置为禁言
//                                    ChatClient.getInstance().groupManager()
//                                        .asyncMuteGroupMembers(groupId, listOf(userId), 3600 *3600 * 2400, object : EMValueCallBack<EMGroup> {
//                                            override fun onSuccess(value: EMGroup?) {
////                                                muteUserIdList.add(userId)
//                                            }
//
//                                            override fun onError(code: Int, error: String?) {
////                                                Toast.makeText(mContext, "禁言失败: $error", Toast.LENGTH_SHORT).show()
//                                            }
//                                        })

                                }
                                else {
                                    muteUserIdList.remove(userId)
//                                    // ✅ 取消禁言
//                                    ChatClient.getInstance().groupManager()
//                                        .asyncUnMuteGroupMembers(groupId, listOf(userId), object : EMValueCallBack<EMGroup> {
//                                            override fun onSuccess(value: EMGroup?) {
////                                                muteUserIdList.remove(userId)
//                                            }
//
//                                            override fun onError(code: Int, error: String?) {
////                                                Toast.makeText(mContext, "解除禁言失败: $error", Toast.LENGTH_SHORT).show()
//                                            }
//                                        })

                                }
                            }
                        } else {
                            it.switchMute.visibility = View.GONE
                        }
                    }
                }


                if (position == 0 || header != null && adapter is ChatUIKitGroupMemberListAdapter
                    && header != (adapter as ChatUIKitGroupMemberListAdapter).getItem(position - 1)?.initialLetter) {
                    if (!TextUtils.isEmpty(header) && isShowInitLetter) {
                        it.header.visibility = View.VISIBLE
                        it.header.text = header
                    }
                }


            }
        }

    }
}