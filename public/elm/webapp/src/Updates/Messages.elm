module Updates.Messages exposing (..)

import Http
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Resources.Template exposing (TemplateId)
import Models.Resources.Instance exposing (InstanceId)

type UpdateAboutInfoMsg
  = FetchAbout (Result Http.Error AboutInfo)

type UpdateLoginStatusMsg
  = FetchLogin (Result Http.Error UserInfo)
  | FetchLogout (Result Http.Error String)
  | ResumeExistingSession UserInfo

type UpdateErrorsMsg
  = AddError String

type UpdateLoginFormMsg
  = LoginAttempt
  | FailedLoginAttempt
  | LogoutAttempt
  | EnterUserName String
  | EnterPassword String

type UpdateBodyViewMsg
  = ToggleTemplate TemplateId
  | InstanceSelected InstanceId Bool
  | AllInstancesSelected (List InstanceId) Bool
  | InstanceExpanded InstanceId Bool
  | AllInstancesExpanded (List InstanceId) Bool
  | EnterParameterValue InstanceId String String
