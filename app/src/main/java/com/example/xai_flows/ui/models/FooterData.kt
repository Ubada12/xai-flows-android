package com.example.xai_flows.ui.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class FooterLink(val name: String, val href: String, val icon: ImageVector)
data class FooterSection(val title: String, val links: List<FooterLink>)
data class SocialLink(val name: String, val href: String, val icon: ImageVector, val color: Color)
data class CompanyInfo(val name: String, val description: String, val email: String, val phone: String, val address: String)

val COMPANY_INFO = CompanyInfo(
    name = "XAI-FLOWS",
    description = "Advanced AI-powered flood prediction and monitoring system providing real-time insights for safer communities.",
    email = "contact@xai-flows.com",
    phone = "+1 (555) 123-4567",
    address = "123 Innovation Drive, Tech City, TC 12345"
)

val FOOTER_SECTIONS = listOf(
    FooterSection(
        "Platform",
        listOf(
            FooterLink("Dashboard", "/", Icons.Filled.Home),
            FooterLink("Predictions", "/predictions", Icons.Filled.BarChart),
            FooterLink("Analytics", "/analytics", Icons.Filled.BarChart),
            FooterLink("About", "/about", Icons.Filled.Info)
        )
    ),
    FooterSection(
        "Company",
        listOf(
            FooterLink("About Us", "/about", Icons.Filled.People),
            FooterLink("Our Team", "/about/team", Icons.Filled.People),
            FooterLink("Careers", "/careers", Icons.Filled.Work),
            FooterLink("News & Updates", "/news", Icons.Filled.Newspaper)
        )
    ),
    FooterSection(
        "Support",
        listOf(
            FooterLink("Help Center", "/support", Icons.Filled.Help),
            FooterLink("Contact Support", "/contact/support", Icons.Filled.Headset),
            FooterLink("Documentation", "/docs", Icons.Filled.Description),
            FooterLink("API Reference", "/api-docs", Icons.Filled.Code)
        )
    ),
    FooterSection(
        "Legal",
        listOf(
            FooterLink("Privacy Policy", "/privacy", Icons.Filled.Security),
            FooterLink("Terms of Service", "/terms", Icons.Filled.Description),
            FooterLink("Cookie Policy", "/cookies", Icons.Filled.Security),
            FooterLink("FAQ", "/faq", Icons.Filled.Help)
        )
    )
)

val SOCIAL_LINKS = listOf(
    SocialLink("Facebook", "https://facebook.com/xaiflows", Icons.Filled.Facebook, Color(0xFF3b5998)),
    SocialLink("Twitter", "https://twitter.com/xaiflows", Icons.Filled.Share, Color(0xFF1DA1F2)),
    SocialLink("LinkedIn", "https://linkedin.com/company/xaiflows", Icons.Filled.Work, Color(0xFF0077B5)),
    SocialLink("Instagram", "https://instagram.com/xaiflows", Icons.Filled.Favorite, Color(0xFFE1306C)),
    SocialLink("GitHub", "https://github.com/xaiflows", Icons.Filled.Code, Color(0xFF6e5494))
)
