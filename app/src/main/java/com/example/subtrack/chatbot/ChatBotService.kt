package com.example.subtrack.chatbot

import com.example.subtrack.Subscription
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.util.Locale

class ChatBotService {
    private val api: HuggingFaceApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api-inference.huggingface.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(HuggingFaceApi::class.java)
    }

    suspend fun generateFinancialAdvice(
        userMessage: String,
        userSubscriptions: List<Subscription>,
        userEmail: String
    ): String {
        val totalMonthlySpending = userSubscriptions.sumOf { it.amount }
        val subscriptionCategories = userSubscriptions.groupBy { it.category }
        val categorySpending = subscriptionCategories.mapValues { (_, subs) ->
            subs.sumOf { it.amount }
        }
        val userName = userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        
        // Use intelligent local analysis instead of external API
        return generateIntelligentAdvice(
            userMessage,
            totalMonthlySpending,
            categorySpending,
            userSubscriptions,
            userName
        )
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
        userName: String
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        val annualSpending = totalMonthlySpending * 12
        
        // Analyze spending patterns
        val highestCategory = categorySpending.maxByOrNull { it.value }
        val unusedSubscriptions = userSubscriptions.filter { 
            // Simple heuristic: if subscription is very cheap relative to others, might be unused
            it.amount < totalMonthlySpending * 0.1 && totalMonthlySpending > 50
        }
        
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
                generateGeneralAdvice(userName, totalMonthlySpending, annualSpending, categorySpending.size, formatter)
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
}