module Updates.Messages exposing (..)

import Http
import Dict exposing (Dict)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Resources.Template exposing (TemplateId, Template)
import Models.Resources.Instance exposing (InstanceId, Instance)

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
  | EnterEditInstanceParameterValue Instance String String
  | EnterNewInstanceParameterValue Template String String
  | ApplyParameterValueChanges Instance
  | DiscardParameterValueChanges Instance
  | ToggleEditInstanceSecretVisibility InstanceId String
  | ToggleNewInstanceSecretVisibility TemplateId String
  | ExpandNewInstanceForm Bool TemplateId
  | SubmitNewInstanceCreation TemplateId (Dict String String)
  | DiscardNewInstanceCreation TemplateId
