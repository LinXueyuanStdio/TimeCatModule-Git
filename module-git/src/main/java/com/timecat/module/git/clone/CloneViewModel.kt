package com.timecat.module.git.clone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.extend.arms.BaseApplication
import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.PreferenceHelper
import com.timecat.module.git.tasks.CloneTask
import com.timecat.module.git.tasks.InitLocalTask

class CloneViewModel(application: Application) : AndroidViewModel(application) {

    var remoteUrl: String = ""
        set(value) {
            field = value
            localRepoName.value = stripGitExtension(stripUrlFromRepo(remoteUrl))
        }

    val localRepoName: MutableLiveData<String> = MutableLiveData()
    var cloneRecursively: Boolean = false
    val initLocal: MutableLiveData<Boolean> = MutableLiveData()

    var remoteUrlError: MutableLiveData<String?> = MutableLiveData()
    var localRepoNameError: MutableLiveData<String?> = MutableLiveData()

    val visible: MutableLiveData<Boolean> = MutableLiveData()

    init {
        visible.value = false
        initLocal.value = false
    }

    fun show(show: Boolean) {
        visible.value = show
    }


    fun cloneRepo() {
        // FIXME: createRepo should not use user visible strings, instead will need to be refactored
        // to set an observable state
        if (initLocal.value as Boolean) {
            LogUtil.d("INIT LOCAL ${localRepoName.value}")
            initLocalRepo()
        } else {
            LogUtil.d("CLONE REPO ${localRepoName.value} ${remoteUrl} [${cloneRecursively}]")
            val repo = Repo.createRepo(localRepoName.value, remoteUrl, "")
            val task = CloneTask(repo, cloneRecursively, "", null)
            task.executeTask()
            remoteUrl = ""
            show(false)
        }
    }

    fun validate(): Boolean {
        return if (initLocal.value as Boolean) {
            validateLocalName(localRepoName.value as String)
        } else validateRemoteUrl(remoteUrl) && validateLocalName(localRepoName.value as String)
    }

    fun initLocalRepo() {
        val repo = Repo.createRepo(localRepoName.value, "local repository", "")
        val task = InitLocalTask(repo)
        task.executeTask()
    }

    private fun stripUrlFromRepo(remoteUrl: String): String {
        val lastSlash = remoteUrl.lastIndexOf("/")
        return if (lastSlash != -1) {
            remoteUrl.substring(lastSlash + 1)
        } else remoteUrl

    }

    private fun stripGitExtension(remoteUrl: String): String {
        val extension = remoteUrl.indexOf(".git")
        return if (extension != -1) {
            remoteUrl.substring(0, extension)
        } else remoteUrl

    }


    private fun validateRemoteUrl(remoteUrl: String): Boolean {
        remoteUrlError.value = null
        if (remoteUrl.isBlank()) {
            remoteUrlError.value = getApplication<BaseApplication>().getString(R.string.git_alert_remoteurl_required)
            return false
        }
        return true
    }

    private fun validateLocalName(localName: String): Boolean {
        localRepoNameError.value = null
        if (localName.isBlank()) {
            localRepoNameError.value =
                getApplication<BaseApplication>().getString((R.string.git_alert_localpath_required))
            return false
        }
        if (localName.contains("/")) {
            localRepoNameError.value =
                getApplication<BaseApplication>().getString((R.string.git_alert_localpath_format))
            return false
        }

        val prefsHelper = PreferenceHelper.getInstance()
        val file = Repo.getDir(prefsHelper, localName)
        if (file.exists()) {
            localRepoNameError.value =
                getApplication<BaseApplication>().getString((R.string.git_alert_localpath_repo_exists))
            return false
        }
        return true
    }
}
