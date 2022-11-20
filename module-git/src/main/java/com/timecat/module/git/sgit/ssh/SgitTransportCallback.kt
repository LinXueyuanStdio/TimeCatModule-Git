package com.timecat.module.git.sgit.ssh

import org.eclipse.jgit.api.TransportConfigCallback
import com.timecat.module.git.sgit.ssh.SGitSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport

/**
 * Created by sheimi on 8/22/13.
 */
class SgitTransportCallback : TransportConfigCallback {
    private val ssh: SGitSessionFactory = SGitSessionFactory()

    override fun configure(tn: Transport) {
        if (tn is SshTransport) {
            tn.sshSessionFactory = ssh
        }
    }
}