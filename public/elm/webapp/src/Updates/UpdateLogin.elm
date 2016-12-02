module Updates.UpdateLogin exposing (updateLogin)

import Updates.Messages exposing (UpdateLoginMsg(..))
import Messages exposing (AnyMsg)
import Models.Ui.Notifications exposing (Errors)

updateLogin : UpdateLoginMsg -> Bool -> (Bool, Cmd AnyMsg)
updateLogin message oldLoggedIn =
  case message of
    LoginAttempt credentials ->
      (oldLoggedIn, Cmd.none)
    LogoutAttempt ->
      (oldLoggedIn, Cmd.none)
    Login ->
      (True, Cmd.none)
    Logout ->
      (False, Cmd.none)
