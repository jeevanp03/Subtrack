package com.example.subtrack.chatbot

import com.example.subtrack.Subscription
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.util.Locale
import android.util.Log
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ChatBotService {
    private val api: HuggingFaceApi

    init {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api-inference.huggingface.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        
        api = retrofit.create(HuggingFaceApi::class.java)
    }

    suspend fun generateFinancialAdvice(
        userMessage: String,
        userSubscriptions: List<Subscription>,
        userEmail: String,
        conversationHistory: List<com.example.subtrack.ChatMessage> = emptyList()
    ): String {
        val totalMonthlySpending = userSubscriptions.sumOf { it.amount }
        val subscriptionCategories = userSubscriptions.groupBy { it.category }
        val categorySpending = subscriptionCategories.mapValues { (_, subs) ->
            subs.sumOf { it.amount }
        }
        val userName = userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        
        return try {
            // Try using HuggingFace LLM first
            generateLLMResponse(
                userMessage,
                totalMonthlySpending,
                categorySpending,
                userSubscriptions,
                userName,
                conversationHistory
            )
        } catch (e: Exception) {
            // Log the error for debugging
            Log.w("ChatBotService", "LLM failed, using fallback: ${e.message}")
            
            // Fallback to local analysis if LLM fails
            generateIntelligentAdvice(
                userMessage,
                totalMonthlySpending,
                categorySpending,
                userSubscriptions,
                userName,
                conversationHistory
            )
        }
    }

    private suspend fun generateLLMResponse(
        userMessage: String,
        totalMonthlySpending: Double,
        categorySpending: Map<String, Double>,
        userSubscriptions: List<Subscription>,
        userName: String,
        conversationHistory: List<com.example.subtrack.ChatMessage> = emptyList()
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        
        // Build context for the LLM
        val context = buildLLMContext(
            userMessage,
            totalMonthlySpending,
            categorySpending,
            userSubscriptions.size,
            userName,
            conversationHistory,
            formatter
        )
        
        // Use a small, fast, free HuggingFace model
        val request = HuggingFaceRequest(
            inputs = context,
            parameters = mapOf(
                "max_new_tokens" to 150,
                "temperature" to 0.8,
                "do_sample" to true,
                "return_full_text" to false,
                "repetition_penalty" to 1.2
            )
        )
        
        Log.d("ChatBotService", "Sending request to HuggingFace: ${context.take(100)}...")
        
        val response = api.generateText("google/flan-t5-small", request)
        
        Log.d("ChatBotService", "API Response: ${response.code()}, Body: ${response.body()}")
        
        if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
            val generatedText = response.body()?.first()?.generated_text?.trim()
            Log.d("ChatBotService", "Generated text: $generatedText")
            
            return if (!generatedText.isNullOrBlank()) {
                // Clean up the response and add personalization
                cleanAndPersonalizeResponse(generatedText, userName, formatter, totalMonthlySpending)
            } else {
                throw Exception("Empty response from LLM")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e("ChatBotService", "API Error: ${response.code()}, Body: $errorBody")
            throw Exception("API call failed: ${response.code()} - $errorBody")
        }
    }

    private fun buildLLMContext(
        userMessage: String,
        totalMonthlySpending: Double,
        categorySpending: Map<String, Double>,
        subscriptionCount: Int,
        userName: String,
        conversationHistory: List<com.example.subtrack.ChatMessage>,
        formatter: NumberFormat
    ): String {
        val recentMessages = conversationHistory.takeLast(3)
        val conversationContext = if (recentMessages.isNotEmpty()) {
            "Previous conversation:\n" + recentMessages.joinToString("\n") { 
                "${if (it.isFromUser) "User" else "Assistant"}: ${it.message}" 
            } + "\n\n"
        } else ""
        
        val topCategory = categorySpending.maxByOrNull { it.value }
        val expensiveOnes = categorySpending.filter { it.value > 30 }
        
        return """${conversationContext}Context: $userName is a user who spends ${formatter.format(totalMonthlySpending)} per month on $subscriptionCount subscriptions${if (topCategory != null) ". Their biggest expense is ${topCategory.key} at ${formatter.format(topCategory.value)}" else ""}${if (expensiveOnes.isNotEmpty()) ". Expensive categories: ${expensiveOnes.entries.joinToString(", ") { "${it.key} (${formatter.format(it.value)})" }}" else ""}.

User question: "$userMessage"

Instructions: Give $userName specific, actionable advice about their subscription spending. Be helpful, concise (2-3 sentences), and personal. Include dollar amounts when relevant."""
    }

    private fun cleanAndPersonalizeResponse(
        response: String,
        userName: String,
        formatter: NumberFormat,
        totalSpending: Double
    ): String {
        var cleaned = response
            .replace(Regex("\\b(FinBot|Assistant|AI):\\s*"), "")
            .replace(Regex("^(User|Human):\\s*.*\\n", RegexOption.MULTILINE), "")
            .trim()
        
        // Ensure the response starts with a greeting if it doesn't already
        if (!cleaned.contains(userName, ignoreCase = true) && !cleaned.startsWith("Hi") && !cleaned.startsWith("Hello")) {
            cleaned = "Hi $userName! $cleaned"
        }
        
        // Add a relevant tip based on spending if response is too generic
        if (cleaned.length < 50 && totalSpending > 0) {
            cleaned += " With ${formatter.format(totalSpending)}/month in subscriptions, consider reviewing which ones you actually use regularly."
        }
        
        return cleaned
    }

    private fun buildFinancialContext(
        userMessage: String,
        totalMonthlySpending: Double,
        categorySpending: Map<String, Double>,
        subscriptionCount: Int,
        userEmail: String
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        val userName = userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        
        return """
            You are a helpful financial advisor focused on helping users save money and live frugally. 
            
            User Profile:
            - Name: $userName
            - Email: $userEmail
            
            Current Financial Situation:
            - Total monthly subscription spending: ${formatter.format(totalMonthlySpending)}
            - Number of active subscriptions: $subscriptionCount
            - Spending breakdown by category: ${if (categorySpending.isNotEmpty()) categorySpending.map { "${it.key}: ${formatter.format(it.value)}" }.joinToString(", ") else "No subscriptions yet"}
            
            User's question: $userMessage
            
            Please provide personalized, practical, and actionable advice to help $userName save money and be more frugal. Address them by name and reference their specific spending patterns when relevant. Keep your response concise and friendly.
            
            Response:
        """.trimIndent()
    }

    private fun generateIntelligentAdvice(
        userMessage: String,
        totalMonthlySpending: Double,
        categorySpending: Map<String, Double>,
        userSubscriptions: List<Subscription>,
        userName: String,
        conversationHistory: List<com.example.subtrack.ChatMessage> = emptyList()
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        val annualSpending = totalMonthlySpending * 12
        
        // Analyze spending patterns
        val highestCategory = categorySpending.maxByOrNull { it.value }
        val unusedSubscriptions = userSubscriptions.filter { 
            // Simple heuristic: if subscription is very cheap relative to others, might be unused
            it.amount < totalMonthlySpending * 0.1 && totalMonthlySpending > 50
        }
        
        // Analyze conversation context
        val recentTopics = conversationHistory.takeLast(4).map { it.message.lowercase() }
        val hasDiscussedBudget = recentTopics.any { it.contains("budget") }
        val hasDiscussedSaving = recentTopics.any { it.contains("save") || it.contains("money") }
        val hasDiscussedCanceling = recentTopics.any { it.contains("cancel") || it.contains("expensive") }
        
        // Determine advice type based on user message
        return when {
            userMessage.contains("save", ignoreCase = true) || userMessage.contains("money", ignoreCase = true) -> {
                generateSavingAdvice(userName, totalMonthlySpending, annualSpending, highestCategory, unusedSubscriptions, formatter)
            }
            userMessage.contains("budget", ignoreCase = true) || userMessage.contains("plan", ignoreCase = true) -> {
                generateBudgetingAdvice(userName, totalMonthlySpending, categorySpending, formatter)
            }
            userMessage.contains("subscription", ignoreCase = true) || userMessage.contains("cancel", ignoreCase = true) -> {
                generateSubscriptionAdvice(userName, userSubscriptions, totalMonthlySpending, unusedSubscriptions, formatter)
            }
            userMessage.contains("expensive", ignoreCase = true) || userMessage.contains("cost", ignoreCase = true) -> {
                generateCostAnalysisAdvice(userName, totalMonthlySpending, annualSpending, highestCategory, formatter)
            }
            userMessage.contains("deal", ignoreCase = true) || userMessage.contains("discount", ignoreCase = true) -> {
                generateDiscountAdvice(userName, userSubscriptions, formatter)
            }
            userMessage.contains("alternative", ignoreCase = true) || userMessage.contains("free", ignoreCase = true) -> {
                generateAlternativeAdvice(userName, categorySpending, formatter)
            }
            else -> {
                // Generate proactive insights for general questions
                val insights = generateProactiveInsights(userSubscriptions, totalMonthlySpending, categorySpending, formatter)
                if (insights.isNotEmpty()) {
                    "$insights\n\n${generateGeneralAdvice(userName, totalMonthlySpending, annualSpending, categorySpending.size, formatter)}"
                } else {
                    generateGeneralAdvice(userName, totalMonthlySpending, annualSpending, categorySpending.size, formatter)
                }
            }
        }
    }
    
    private fun generateSavingAdvice(
        userName: String,
        monthlySpending: Double,
        annualSpending: Double,
        highestCategory: Map.Entry<String, Double>?,
        unusedSubs: List<Subscription>,
        formatter: NumberFormat
    ): String {
        val savings = when {
            monthlySpending > 100 -> "20-30%"
            monthlySpending > 50 -> "15-25%"
            else -> "10-20%"
        }
        
        return buildString {
            append("Hi $userName! Great question about saving money. ")
            append("You're spending ${formatter.format(monthlySpending)}/month (${formatter.format(annualSpending)}/year) on subscriptions. ")
            
            if (highestCategory != null) {
                append("Your biggest expense is ${highestCategory.key} at ${formatter.format(highestCategory.value)}/month. ")
            }
            
            append("Here's how you could save $savings:\n\n")
            
            if (unusedSubs.isNotEmpty()) {
                append("🔍 **Audit unused services**: You have ${unusedSubs.size} small subscriptions that might be forgotten.\n")
            }
            
            append("💡 **Quick wins**:\n")
            append("• Cancel trials you forgot about\n")
            append("• Switch to annual billing (usually 10-20% cheaper)\n")
            append("• Share family plans with friends/family\n")
            
            if (highestCategory != null) {
                append("• Look for cheaper alternatives in ${highestCategory.key}\n")
            }
            
            append("\nWould you like specific advice for any category?")
        }
    }
    
    private fun generateBudgetingAdvice(
        userName: String,
        monthlySpending: Double,
        categorySpending: Map<String, Double>,
        formatter: NumberFormat
    ): String {
        val recommendedLimit = when {
            monthlySpending > 200 -> monthlySpending * 0.7
            monthlySpending > 100 -> monthlySpending * 0.8
            else -> monthlySpending * 0.9
        }
        
        return buildString {
            append("Hi $userName! Let's create a smart subscription budget. ")
            append("Currently spending: ${formatter.format(monthlySpending)}/month\n\n")
            
            append("📊 **Your spending breakdown**:\n")
            categorySpending.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
                val percentage = (amount / monthlySpending * 100).toInt()
                append("• $category: ${formatter.format(amount)} ($percentage%)\n")
            }
            
            append("\n🎯 **Recommended budget**: ${formatter.format(recommendedLimit)}/month\n")
            append("This saves you ${formatter.format(monthlySpending - recommendedLimit)}/month!\n\n")
            
            append("💼 **Budgeting tips**:\n")
            append("• Set spending alerts for each category\n")
            append("• Use the 50/30/20 rule for overall finances\n")
            append("• Review subscriptions monthly\n")
            append("• Track your subscription-to-income ratio (aim for <10%)")
        }
    }
    
    private fun generateSubscriptionAdvice(
        userName: String,
        subscriptions: List<Subscription>,
        totalSpending: Double,
        unusedSubs: List<Subscription>,
        formatter: NumberFormat
    ): String {
        val expensiveOnes = subscriptions.filter { it.amount > totalSpending * 0.3 }
        
        return buildString {
            append("Hi $userName! Let's optimize your ${subscriptions.size} subscriptions. ")
            append("Total: ${formatter.format(totalSpending)}/month\n\n")
            
            if (expensiveOnes.isNotEmpty()) {
                append("💰 **Most expensive** (${expensiveOnes.size}):\n")
                expensiveOnes.forEach { sub ->
                    append("• ${sub.name}: ${formatter.format(sub.amount)}/month\n")
                }
                append("\n")
            }
            
            if (unusedSubs.isNotEmpty()) {
                append("⚠️ **Possibly unused** (${unusedSubs.size}):\n")
                unusedSubs.forEach { sub ->
                    append("• ${sub.name}: ${formatter.format(sub.amount)}/month\n")
                }
                append("\n")
            }
            
            append("🔧 **Action plan**:\n")
            append("• Cancel or downgrade expensive services you rarely use\n")
            append("• Look for bundle deals\n")
            append("• Set usage reminders for borderline subscriptions\n")
            append("• Consider seasonal subscriptions (pause when not needed)\n\n")
            
            append("Which subscription would you like help analyzing first?")
        }
    }
    
    private fun generateCostAnalysisAdvice(
        userName: String,
        monthlySpending: Double,
        annualSpending: Double,
        highestCategory: Map.Entry<String, Double>?,
        formatter: NumberFormat
    ): String {
        return buildString {
            append("Hi $userName! Let's break down your subscription costs:\n\n")
            
            append("📈 **Cost analysis**:\n")
            append("• Monthly: ${formatter.format(monthlySpending)}\n")
            append("• Annual: ${formatter.format(annualSpending)}\n")
            append("• Daily: ${formatter.format(monthlySpending / 30)}\n\n")
            
            if (highestCategory != null) {
                val percentage = (highestCategory.value / monthlySpending * 100).toInt()
                append("🎯 **Biggest expense**: ${highestCategory.key} (${formatter.format(highestCategory.value)}, $percentage%)\n\n")
            }
            
            append("💡 **Cost-cutting priorities**:\n")
            append("1. Target services over $20/month first\n")
            append("2. Look for annual discounts (save 10-20%)\n")
            append("3. Consider shared plans\n")
            append("4. Negotiate with providers\n\n")
            
            append("Fun fact: Cutting just $20/month saves $240/year! 💪")
        }
    }
    
    private fun generateDiscountAdvice(
        userName: String,
        subscriptions: List<Subscription>,
        formatter: NumberFormat
    ): String {
        return buildString {
            append("Hi $userName! Here are ways to get discounts on your subscriptions:\n\n")
            
            append("🎁 **Discount strategies**:\n")
            append("• **Annual billing**: Usually 10-20% off monthly rates\n")
            append("• **Student discounts**: Many services offer 50% off\n")
            append("• **Family plans**: Share costs with up to 6 people\n")
            append("• **Bundle deals**: Combine services for savings\n")
            append("• **Loyalty programs**: Long-term customer discounts\n\n")
            
            append("📞 **Negotiation tips**:\n")
            append("• Call retention departments\n")
            append("• Mention competitor pricing\n")
            append("• Threaten to cancel (politely)\n")
            append("• Ask about promotional rates\n\n")
            
            append("🔍 **Timing matters**:\n")
            append("• Black Friday / Cyber Monday deals\n")
            append("• End of quarter promotions\n")
            append("• Back-to-school seasons\n\n")
            
            append("Which subscription would you like help negotiating?")
        }
    }
    
    private fun generateAlternativeAdvice(
        userName: String,
        categorySpending: Map<String, Double>,
        formatter: NumberFormat
    ): String {
        return buildString {
            append("Hi $userName! Here are free/cheaper alternatives by category:\n\n")
            
            categorySpending.forEach { (category, spending) ->
                append("💡 **$category** (${formatter.format(spending)}/month):\n")
                when (category.lowercase()) {
                    "streaming", "entertainment", "video" -> {
                        append("• Free: YouTube, Tubi, Crackle, Pluto TV\n")
                        append("• Cheaper: Apple TV+ ($5), Paramount+ ($6)\n")
                    }
                    "music", "audio" -> {
                        append("• Free: Spotify Free, YouTube Music Free\n")
                        append("• Cheaper: Apple Music Student ($5)\n")
                    }
                    "news", "reading" -> {
                        append("• Free: Library apps, BBC, NPR\n")
                        append("• Cheaper: Bundle with Amazon Prime\n")
                    }
                    "fitness", "health" -> {
                        append("• Free: YouTube workouts, fitness apps\n")
                        append("• Cheaper: Local gym memberships\n")
                    }
                    "productivity", "software" -> {
                        append("• Free: Google Workspace, LibreOffice\n")
                        append("• Cheaper: Educational discounts\n")
                    }
                    else -> {
                        append("• Research free open-source alternatives\n")
                        append("• Check if your library offers access\n")
                    }
                }
                append("\n")
            }
            
            append("Which category interests you most for alternatives?")
        }
    }
    
    private fun generateGeneralAdvice(
        userName: String,
        monthlySpending: Double,
        annualSpending: Double,
        categoryCount: Int,
        formatter: NumberFormat
    ): String {
        return buildString {
            append("Hi $userName! 👋 I'm your personal financial assistant.\n\n")
            
            append("📊 **Your subscription overview**:\n")
            append("• Monthly spending: ${formatter.format(monthlySpending)}\n")
            append("• Annual cost: ${formatter.format(annualSpending)}\n")
            append("• Active subscriptions: $categoryCount categories\n\n")
            
            append("💰 **I can help you with**:\n")
            append("• Finding ways to save money\n")
            append("• Creating a subscription budget\n")
            append("• Analyzing your costs\n")
            append("• Finding discounts and deals\n")
            append("• Suggesting free alternatives\n")
            append("• Optimizing your subscriptions\n\n")
            
            when {
                monthlySpending > 150 -> append("💡 With your spending level, you could easily save $30-50/month!")
                monthlySpending > 75 -> append("💡 You have good potential for saving $15-25/month!")
                else -> append("💡 Great job keeping your subscriptions lean!")
            }
            
            append("\n\nWhat would you like help with today?")
        }
    }
    
    private fun generateProactiveInsights(
        subscriptions: List<Subscription>,
        totalSpending: Double,
        categorySpending: Map<String, Double>,
        formatter: NumberFormat
    ): String {
        val insights = mutableListOf<String>()
        
        // Check for expensive subscriptions
        val expensiveOnes = subscriptions.filter { it.amount > 50 }
        if (expensiveOnes.isNotEmpty()) {
            insights.add("💡 **Insight**: You have ${expensiveOnes.size} subscription(s) over $50/month totaling ${formatter.format(expensiveOnes.sumOf { it.amount })}.")
        }
        
        // Check for potential annual savings
        if (totalSpending > 100) {
            val annualSavings = totalSpending * 12 * 0.15
            insights.add("💰 **Potential savings**: Switching to annual billing could save you ~${formatter.format(annualSavings)}/year!")
        }
        
        // Check category balance
        val streamingSpending = categorySpending["Streaming"] ?: 0.0
        if (streamingSpending > totalSpending * 0.5) {
            insights.add("📺 **Category alert**: ${((streamingSpending/totalSpending)*100).toInt()}% of your spending is on streaming services.")
        }
        
        // Check for small subscriptions that might be forgotten
        val smallSubs = subscriptions.filter { it.amount < 10 }
        if (smallSubs.size >= 3) {
            insights.add("🔍 **Review needed**: You have ${smallSubs.size} subscriptions under $10 - consider if you still use them all.")
        }
        
        return if (insights.isNotEmpty()) {
            "🚨 **Quick Insights**:\n${insights.joinToString("\n")}\n"
        } else ""
    }
}