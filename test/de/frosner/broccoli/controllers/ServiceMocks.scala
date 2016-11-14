package de.frosner.broccoli.controllers

import de.frosner.broccoli.conf
import de.frosner.broccoli.services.SecurityService
import org.mockito.Mockito._

trait ServiceMocks {

  def withAuthConf(securityService: SecurityService, allowed: Iterable[Account]): SecurityService = {
    allowed.foreach { account =>
      when(securityService.getAccount(account.name)).thenReturn(Some(account))
      when(securityService.isAllowedToAuthenticate(account)).thenReturn(true)
    }
    when(securityService.authMode).thenReturn(conf.AUTH_MODE_CONF)
    securityService
  }

  def withAuthNone(securityService: SecurityService): SecurityService = {
    when(securityService.authMode).thenReturn(conf.AUTH_MODE_NONE)
    securityService
  }

}
