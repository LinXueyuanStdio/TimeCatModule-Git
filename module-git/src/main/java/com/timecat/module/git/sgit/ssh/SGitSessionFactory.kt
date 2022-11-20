package com.timecat.module.git.sgit.ssh

import com.jcraft.jsch.*
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils.migratePrivateKeys
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils.privateKeyFolder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.CredentialsProviderUserInfo
import com.timecat.module.git.sgit.AndroidJschCredentialsProvider
import com.timecat.module.git.utils.SecurePrefsHelper
import kotlin.Throws
import org.eclipse.jgit.util.FS

/**
 * Custom config for Jsch, including using user-provided private keys
 */
class SGitSessionFactory : JschConfigSessionFactory() {
    override fun configure(host: OpenSshConfig.Host, session: Session) {
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("PreferredAuthentications", "publickey,password")

        // Awful use of App singleton but not really any other way to get hold of a provider that needs
        // to have been initialised with an Android context
        val userInfo: UserInfo = CredentialsProviderUserInfo(
            session,
            AndroidJschCredentialsProvider.getInstance(SecurePrefsHelper.getInstance())
        )
        session.userInfo = userInfo
    }

    @Throws(JSchException::class)
    override fun createDefaultJSch(fs: FS): JSch {
        val jsch = JSch()
        migratePrivateKeys()
        val sshDir = privateKeyFolder
        for (file in sshDir.listFiles()) {
            val kpair = KeyPair.load(jsch, file.absolutePath)
            jsch.addIdentity(file.absolutePath)
        }
        return jsch
    }
}