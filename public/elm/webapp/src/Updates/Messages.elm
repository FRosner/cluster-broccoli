module Updates.Messages exposing (..)

import Http
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Ui.Credentials exposing (Credentials)

type UpdateAboutInfoMsg
  = FetchAbout (Result Http.Error (AboutInfo))

type UpdateErrorsMsg
  = AddError String

type UpdateLoginMsg
  = LoginAttempt Credentials
  | LogoutAttempt
  | Login
  | Logout
