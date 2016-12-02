module Updates.Messages exposing (..)

import Http
import Models.Resources.AboutInfo exposing (AboutInfo)

type UpdateAboutInfoMsg
  = FetchAbout (Result Http.Error (AboutInfo))

type UpdateErrorsMsg
  = AddError String

type UpdateLoginFormMsg
  = LoginAttempt
  | EnterUserName String
  | EnterPassword String
