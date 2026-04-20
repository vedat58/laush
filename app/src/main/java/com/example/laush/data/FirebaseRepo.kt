package com.example.laush.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.laush.model.User
import com.example.laush.model.Post
import com.example.laush.model.Comment
import com.example.laush.model.ChatRoom
import com.example.laush.model.Message
import com.example.laush.model.Notification
import com.example.laush.model.Story
import com.example.laush.model.Poll
import kotlinx.coroutines.tasks.await

class FirebaseRepo {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun register(username: String, password: String, displayName: String): Result<User> {
        return try {
            val email = "$username@laush.com"
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User creation failed")
            
            val countSnap = db.collection("users").document("counter").get().await()
            val count = (countSnap.getLong("count") ?: 0) + 1
            db.collection("users").document("counter").set(mapOf("count" to count)).await()
            
            val userData = mapOf(
                "id" to uid,
                "email" to email,
                "username" to username,
                "displayName" to displayName,
                "bio" to "",
                "userNumber" to count,
                "followers" to 0,
                "following" to 0,
                "posts" to 0,
                "createdAt" to System.currentTimeMillis()
            )
            
            db.collection("users").document(uid).set(userData).await()
            
            Result.success(User(
                id = uid,
                username = username,
                displayName = displayName,
                email = email,
                userNumber = count.toString()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val email = "$username@laush.com"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            
            val doc = db.collection("users").document(uid).get().await()
            Result.success(User(
                id = uid,
                email = doc.getString("email") ?: "",
                username = doc.getString("username") ?: "",
                displayName = doc.getString("displayName") ?: "",
                userNumber = doc.getLong("userNumber")?.toString() ?: ""
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(): Result<User> {
        return try {
            // Google Sign In will be handled by Activity
            Result.failure(Exception("Use Google Sign In Intent"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateUser(uid: String, email: String, displayName: String, photoUrl: String?): Result<User> {
        return try {
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            
            val userNumber = if (!snapshot.exists()) {
                val countSnap = db.collection("users").document("counter").get().await()
                val count = (countSnap.getLong("count") ?: 0) + 1
                db.collection("users").document("counter").set(mapOf("count" to count)).await()
                count
            } else {
                snapshot.getLong("userNumber") ?: 0
            }

            val userData = mapOf(
                "id" to uid,
                "email" to email,
                "username" to displayName.lowercase().replace(" ", "_"),
                "displayName" to displayName,
                "bio" to "",
                "photoUrl" to photoUrl,
                "userNumber" to userNumber,
                "followers" to 0,
                "following" to 0,
                "posts" to 0,
                "createdAt" to System.currentTimeMillis()
            )
            
            userRef.set(userData).await()
            
            Result.success(User(
                id = uid,
                username = displayName.lowercase().replace(" ", "_"),
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                userNumber = userNumber.toString()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): User? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            User(
                id = uid,
                email = doc.getString("email") ?: "",
                username = doc.getString("username") ?: "",
                passwordHash = "",
                displayName = doc.getString("displayName") ?: "",
                bio = doc.getString("bio") ?: "",
                photoUrl = doc.getString("photoUrl"),
                userNumber = doc.getLong("userNumber")?.toString() ?: "",
                followers = (doc.getLong("followers") ?: 0).toInt(),
                following = (doc.getLong("following") ?: 0).toInt(),
                posts = (doc.getLong("posts") ?: 0).toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByNumber(userNumber: String): User? {
        return try {
            val query = db.collection("users")
                .whereEqualTo("userNumber", userNumber.toLongOrNull() ?: 0)
                .get()
                .await()
            
            if (query.isEmpty) null
            else {
                val doc = query.documents[0]
                User(
                    id = doc.id,
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    bio = doc.getString("bio") ?: "",
                    photoUrl = doc.getString("photoUrl"),
                    userNumber = doc.getLong("userNumber")?.toString() ?: ""
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchUsers(query: String): List<User> {
        return try {
            val snapshot = db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            snapshot.map { doc ->
                User(
                    id = doc.id,
                    username = doc.getString("username") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl"),
                    userNumber = doc.getLong("userNumber")?.toString() ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateProfile(userId: String, displayName: String, bio: String, photoUrl: String?): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName,
                "bio" to bio
            )
            photoUrl?.let { updates["photoUrl"] = it }
            
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(userId: String, username: String, content: String, imageUrl: String? = null, videoUrl: String? = null, category: String = ""): Result<String> {
        return try {
            val doc = db.collection("posts").document()
            val postData = mapOf(
                "id" to doc.id,
                "userId" to userId,
                "username" to username,
                "content" to content,
                "imageUrl" to imageUrl,
                "videoUrl" to videoUrl,
                "category" to category,
                "likes" to 0,
                "comments" to 0,
                "createdAt" to System.currentTimeMillis()
            )
            doc.set(postData).await()
            
            db.collection("users").document(userId)
                .update("posts", com.google.firebase.firestore.FieldValue.increment(1)).await()
            
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeedPosts(): List<Post> {
        return try {
            val docs = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            docs.map { doc ->
                Post(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    username = doc.getString("username") ?: "",
                    content = doc.getString("content") ?: "",
                    imageUrl = doc.getString("imageUrl"),
                    videoUrl = doc.getString("videoUrl"),
                    category = doc.getString("category") ?: "",
                    likes = (doc.getLong("likes") ?: 0).toInt(),
                    comments = (doc.getLong("comments") ?: 0).toInt(),
                    createdAt = doc.getLong("createdAt") ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVideoPosts(): List<Post> {
        return try {
            val docs = db.collection("posts")
                .get()
                .await()
            
            docs.documents
                .filter { it.getString("videoUrl") != null }
                .map { doc ->
                    Post(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        content = doc.getString("content") ?: "",
                        imageUrl = doc.getString("imageUrl"),
                        videoUrl = doc.getString("videoUrl"),
                        category = doc.getString("category") ?: "",
                        likes = (doc.getLong("likes") ?: 0).toInt(),
                        comments = (doc.getLong("comments") ?: 0).toInt(),
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                }
                .sortedByDescending { it.createdAt }
                .take(30)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTextPosts(): List<Post> {
        return try {
            val docs = db.collection("posts")
                .whereEqualTo("isTextPost", true)
                .get()
                .await()
            
            docs.documents
                .map { doc ->
                    Post(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        content = doc.getString("content") ?: "",
                        imageUrl = doc.getString("imageUrl"),
                        videoUrl = null,
                        category = "",
                        likes = (doc.getLong("likes") ?: 0).toInt(),
                        comments = (doc.getLong("comments") ?: 0).toInt(),
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                }
                .sortedByDescending { it.createdAt }
                .take(30)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deletePost(postId: String): Boolean {
        return try {
            db.collection("posts").document(postId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRandomVideo(): Post? {
        val videos = getVideoPosts()
        return if (videos.isNotEmpty()) videos.random() else null
    }

    suspend fun likePost(postId: String, userId: String): Boolean {
        return try {
            val allLikes = db.collection("likes").get().await()
            
            val existing = allLikes.documents.find { 
                it.getString("postId") == postId && it.getString("userId") == userId 
            }
            
            if (existing == null) {
                db.collection("likes").document().set(mapOf(
                    "postId" to postId,
                    "userId" to userId,
                    "createdAt" to System.currentTimeMillis()
                )).await()
                
                db.collection("posts").document(postId)
                    .update("likes", com.google.firebase.firestore.FieldValue.increment(1)).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addComment(postId: String, userId: String, username: String, content: String): Result<String> {
        return try {
            val doc = db.collection("comments").document()
            doc.set(mapOf(
                "postId" to postId,
                "userId" to userId,
                "username" to username,
                "content" to content,
                "createdAt" to System.currentTimeMillis()
            )).await()
            
            db.collection("posts").document(postId)
                .update("comments", com.google.firebase.firestore.FieldValue.increment(1)).await()
            
            // Send notification
            sendNotification(userId, "$username yorum yaptı: $content")
            
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(postId: String): List<Comment> {
        return try {
            val docs = db.collection("comments")
                .whereEqualTo("postId", postId)
                .get()
                .await()
            
            docs.map { doc ->
                Comment(
                    id = doc.id,
                    postId = postId,
                    userId = doc.getString("userId") ?: "",
                    username = doc.getString("username") ?: "",
                    content = doc.getString("content") ?: "",
                    likes = (doc.getLong("likes") ?: 0).toInt(),
                    createdAt = doc.getLong("createdAt") ?: 0
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun likeComment(commentId: String): Boolean {
        return try {
            db.collection("comments").document(commentId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(1)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Chat/DM functions
    suspend fun getOrCreateChatRoom(userId1: String, userId2: String): String {
        return try {
            val allChats = db.collection("chatRooms").get().await()
            
            val existing = allChats.documents.find { doc ->
                (doc.getString("user1") == userId1 && doc.getString("user2") == userId2) ||
                (doc.getString("user1") == userId2 && doc.getString("user2") == userId1)
            }
            
            if (existing != null) {
                existing.id
            } else {
                val doc = db.collection("chatRooms").document()
                doc.set(mapOf(
                    "user1" to userId1,
                    "user2" to userId2,
                    "createdAt" to System.currentTimeMillis()
                )).await()
                doc.id
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun sendMessage(chatRoomId: String, senderId: String, message: String): Result<String> {
        return try {
            val doc = db.collection("messages").document()
            doc.set(mapOf(
                "chatRoomId" to chatRoomId,
                "senderId" to senderId,
                "message" to message,
                "type" to "text",
                "createdAt" to System.currentTimeMillis()
            )).await()
            
            db.collection("chatRooms").document(chatRoomId)
                .update(mapOf(
                    "lastMessage" to message,
                    "lastMessageAt" to System.currentTimeMillis()
                )).await()
            
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPhotoMessage(chatRoomId: String, senderId: String, uri: android.net.Uri, context: android.content.Context): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Dosya okunamadı")
            inputStream.close()

            val uploadResult = CloudinaryUpload.upload(bytes, "image", "laush_unsigned")
            if (uploadResult == null) {
                return Result.failure(Exception("Fotoğraf yüklenemedi"))
            }

            val doc = db.collection("messages").document()
            doc.set(mapOf(
                "chatRoomId" to chatRoomId,
                "senderId" to senderId,
                "message" to uploadResult,
                "type" to "image",
                "createdAt" to System.currentTimeMillis()
            )).await()
            
            db.collection("chatRooms").document(chatRoomId)
                .update(mapOf(
                    "lastMessage" to "📷 Fotoğraf",
                    "lastMessageAt" to System.currentTimeMillis()
                )).await()
            
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(chatRoomId: String): List<Message> {
        return try {
            val docs = db.collection("messages")
                .whereEqualTo("chatRoomId", chatRoomId)
                .get()
                .await()
            
            docs.map { doc ->
                Message(
                    id = doc.id,
                    chatRoomId = chatRoomId,
                    senderId = doc.getString("senderId") ?: "",
                    message = doc.getString("message") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0
                )
            }.sortedBy { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getChatRooms(userId: String): List<ChatRoom> {
        return try {
            val docs = db.collection("chatRooms").get().await()
            
            docs.documents.filter { doc ->
                doc.getString("user1") == userId || doc.getString("user2") == userId
            }.map { doc ->
                val otherUserId = if (doc.getString("user1") == userId) doc.getString("user2") else doc.getString("user1")
                val otherUser = otherUserId?.let { getUser(it) }
                ChatRoom(
                    id = doc.id,
                    user1 = doc.getString("user1") ?: "",
                    user2 = doc.getString("user2") ?: "",
                    lastMessage = doc.getString("lastMessage") ?: "",
                    lastMessageAt = doc.getLong("lastMessageAt") ?: 0,
                    otherUserName = otherUser?.displayName ?: "",
                    otherUserNumber = otherUser?.userNumber ?: "",
                    otherUserPhoto = otherUser?.photoUrl
                )
            }.sortedByDescending { it.lastMessageAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Group functions
    suspend fun createGroup(name: String, creatorId: String, memberIds: List<String>): Result<String> {
        return try {
            val doc = db.collection("groups").document()
            doc.set(mapOf(
                "name" to name,
                "creatorId" to creatorId,
                "members" to listOf(creatorId) + memberIds,
                "createdAt" to System.currentTimeMillis()
            )).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendGroupMessage(groupId: String, senderId: String, senderName: String, message: String): Result<String> {
        return try {
            val doc = db.collection("groupMessages").document()
            doc.set(mapOf(
                "groupId" to groupId,
                "senderId" to senderId,
                "senderName" to senderName,
                "message" to message,
                "createdAt" to System.currentTimeMillis()
            )).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notifications
    suspend fun sendNotification(userId: String, message: String) {
        try {
            db.collection("notifications").document().set(mapOf(
                "userId" to userId,
                "message" to message,
                "createdAt" to System.currentTimeMillis(),
                "read" to false
            ))
        } catch (e: Exception) { }
    }

    suspend fun getNotifications(userId: String): List<Notification> {
        return try {
            val docs = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            docs.map { doc ->
                Notification(
                    id = doc.id,
                    userId = userId,
                    message = doc.getString("message") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0,
                    read = doc.getBoolean("read") ?: false
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun logout() {
        auth.signOut()
    }

    // Follow system
    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            db.collection("follows").document("${currentUserId}_$targetUserId").set(mapOf(
                "followerId" to currentUserId,
                "followingId" to targetUserId,
                "createdAt" to System.currentTimeMillis()
            )).await()
            
            db.collection("users").document(currentUserId)
                .update("following", FieldValue.increment(1)).await()
            db.collection("users").document(targetUserId)
                .update("followers", FieldValue.increment(1)).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            db.collection("follows").document("${currentUserId}_$targetUserId").delete().await()
            
            db.collection("users").document(currentUserId)
                .update("following", FieldValue.increment(-1)).await()
            db.collection("users").document(targetUserId)
                .update("followers", FieldValue.increment(-1)).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val doc = db.collection("follows").document("${currentUserId}_$targetUserId").get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowings(userId: String): List<String> {
        return try {
            val docs = db.collection("follows")
                .whereEqualTo("followerId", userId)
                .get()
                .await()
            docs.map { it.getString("followingId") ?: "" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun canMessage(senderId: String, receiverId: String): Boolean {
        return isFollowing(senderId, receiverId) || senderId == receiverId
    }

    // Notes
    suspend fun saveNote(userId: String, note: String): Result<Unit> {
        return try {
            db.collection("notes").document(userId).set(mapOf(
                "userId" to userId,
                "note" to note,
                "updatedAt" to System.currentTimeMillis()
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNote(userId: String): String? {
        return try {
            val doc = db.collection("notes").document(userId).get().await()
            doc.getString("note")
        } catch (e: Exception) {
            null
        }
    }

    // Stories
    suspend fun createStory(imageUrl: String?, videoUrl: String?): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        val user = getUser(userId) ?: return Result.failure(Exception("User not found"))
        return try {
            val doc = db.collection("stories").document()
            doc.set(mapOf(
                "id" to doc.id,
                "userId" to userId,
                "username" to user.username,
                "imageUrl" to imageUrl,
                "videoUrl" to videoUrl,
                "createdAt" to System.currentTimeMillis()
            )).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStories(): List<Story> {
        return try {
            val userId = getCurrentUserId() ?: return emptyList()
            val followings = getFollowings(userId)
            val allUserIds = (followings + userId).distinct()

            val stories = mutableListOf<Story>()
            for (uid in allUserIds) {
                val userStories = db.collection("stories")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                    .documents
                    .filter { (System.currentTimeMillis() - (it.getLong("createdAt") ?: 0)) < 24 * 60 * 60 * 1000 }
                    .map { doc ->
                        Story(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            username = doc.getString("username") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            videoUrl = doc.getString("videoUrl"),
                            createdAt = doc.getLong("createdAt") ?: 0
                        )
                    }
                stories.addAll(userStories)
            }
            stories.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Polls
    suspend fun createPoll(question: String, options: List<String>): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        val user = getUser(userId) ?: return Result.failure(Exception("User not found"))
        return try {
            val doc = db.collection("polls").document()
            doc.set(mapOf(
                "id" to doc.id,
                "userId" to userId,
                "username" to user.username,
                "question" to question,
                "options" to options,
                "votes" to options.map { 0 },
                "createdAt" to System.currentTimeMillis()
            )).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun votePoll(pollId: String, optionIndex: Int): Result<Unit> {
        return try {
            db.collection("polls").document(pollId)
                .update("votes", FieldValue.arrayUnion(optionIndex)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPolls(): List<Poll> {
        return try {
            db.collection("polls")
                .get()
                .await()
                .documents
                .map { doc ->
                    Poll(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        question = doc.getString("question") ?: "",
                        options = (doc.get("options") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        votes = (doc.get("votes") as? List<*>)?.mapNotNull { it as? Long }?.map { it.toInt() } ?: emptyList(),
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                }
                .sortedByDescending { it.createdAt }
                .take(30)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Heart/Like - Double tap
    suspend fun toggleLike(postId: String, userId: String): Boolean {
        return try {
            val allLikes = db.collection("likes").get().await()
            val existing = allLikes.documents.find {
                it.getString("postId") == postId && it.getString("userId") == userId
            }
            if (existing != null) {
                existing.reference.delete().await()
                db.collection("posts").document(postId)
                    .update("likes", FieldValue.increment(-1)).await()
                false
            } else {
                db.collection("likes").document().set(mapOf(
                    "postId" to postId,
                    "userId" to userId,
                    "createdAt" to System.currentTimeMillis()
                )).await()
                db.collection("posts").document(postId)
                    .update("likes", FieldValue.increment(1)).await()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // Mega Tick
    suspend fun setMegaVerified(userId: String, verified: Boolean): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("isMegaVerified", verified).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Theme
    suspend fun setTheme(userId: String, theme: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("theme", theme).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}