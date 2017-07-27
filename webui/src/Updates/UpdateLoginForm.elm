module Updates.UpdateLoginForm exposing (updateLoginForm)

import Updates.Messages exposing (UpdateLoginFormMsg(..))
import Messages exposing (AnyMsg(..))
import Commands.LoginLogout
import Models.Ui.LoginForm exposing (LoginForm)


updateLoginForm : UpdateLoginFormMsg -> LoginForm -> ( LoginForm, Cmd AnyMsg )
updateLoginForm message oldLoginForm =
    case message of
        LoginAttempt username password ->
            ( { oldLoginForm
                | loginIncorrect = False
              }
            , Cmd.map UpdateLoginStatusMsg
                (Commands.LoginLogout.loginRequest username password)
            )

        LogoutAttempt ->
            ( oldLoginForm
            , Cmd.map UpdateLoginStatusMsg Commands.LoginLogout.logoutRequest
            )

        EnterUserName newUsername ->
            ( { oldLoginForm | username = newUsername, loginIncorrect = False }
            , Cmd.none
            )

        EnterPassword newPassword ->
            ( { oldLoginForm | password = newPassword, loginIncorrect = False }
            , Cmd.none
            )
