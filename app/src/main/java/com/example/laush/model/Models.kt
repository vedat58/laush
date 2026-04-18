package com.example.laush.model

data class User(
    val id: String = "",
    val username: String = "",
    val passwordHash: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val photoUrl: String? = null,
    val userNumber: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val posts: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val category: String = "",
    val likes: Int = 0,
    val comments: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val likes: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatRoom(
    val id: String = "",
    val user1: String = "",
    val user2: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0,
    val otherUserName: String = "",
    val otherUserNumber: String = "",
    val otherUserPhoto: String? = null
)

data class Message(
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val message: String = "",
    val type: String = "text",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Notification(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)