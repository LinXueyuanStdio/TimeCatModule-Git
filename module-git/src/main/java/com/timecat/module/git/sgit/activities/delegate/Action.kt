package com.timecat.module.git.sgit.activities.delegate

import android.content.Context
import com.timecat.module.git.sgit.database.models.Repo

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2022/11/6
 * @description null
 * @usage null
 */
sealed class Action(val context: Context, val repo: Repo)



