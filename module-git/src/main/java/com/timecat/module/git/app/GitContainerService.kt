package com.timecat.module.git.app

import android.content.Context
import com.timecat.identity.readonly.RouterHub
import com.timecat.middle.block.service.ContainerService
import com.timecat.middle.block.service.HomeService
import com.timecat.module.git.sgit.database.RepoDbManager
import com.timecat.module.git.sgit.database.models.Repo
import com.xiaojinzi.component.anno.ServiceAnno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/1/20
 * @description null
 * @usage null
 */
@ServiceAnno(ContainerService::class, name = [RouterHub.GLOBAL_GitContainerService])
class GitContainerService : ContainerService {
    override fun loadForVirtualPath(context: Context, parentUuid: String, homeService: HomeService, callback: ContainerService.LoadCallback) {
        GlobalScope.launch(Dispatchers.IO) {
            val cursor = RepoDbManager.queryAllRepo()
            val repo = Repo.getRepoList(context, cursor)
            repo.sort()
            cursor.close()
            val cards = repo.map { RepoCard(it, context) }
            withContext(Dispatchers.Main) {
                callback.onVirtualLoadSuccess(cards)
            }
        }
    }
}