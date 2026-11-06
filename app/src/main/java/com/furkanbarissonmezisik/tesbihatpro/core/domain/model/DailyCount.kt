package com.furkanbarissonmezisik.tesbihatpro.core.domain.model

import java.time.LocalDate

data class DailyCount(val date: LocalDate, val zikirId: Long, val count: Int)
