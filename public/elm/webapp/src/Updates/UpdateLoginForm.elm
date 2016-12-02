module Updates.UpdateLoginForm exposing (updateLoginForm)

import Updates.Messages exposing (UpdateLoginFormMsg(..))
import Messages exposing (AnyMsg)
import Models.Ui.LoginForm exposing (LoginForm)

updateLoginForm : UpdateLoginFormMsg -> LoginForm -> (LoginForm, Cmd AnyMsg)
updateLoginForm message oldLoginForm =
  case message of
    LoginAttempt ->
      ( oldLoginForm
      , Cmd.none -- TODO send request
      )
    EnterUserName newUsername ->
      ( { oldLoginForm | username = newUsername }
      , Cmd.none
      )
    EnterPassword newPassword ->
      ( { oldLoginForm | password = newPassword }
      , Cmd.none
      )
