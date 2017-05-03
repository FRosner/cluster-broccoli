module Updates.Messages exposing (..)

import Http

import Dict exposing (Dict)

import Set exposing (Set)

import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Resources.Template exposing (TemplateId, Template)
import Models.Resources.Instance exposing (InstanceId, Instance)

type UpdateAboutInfoMsg
  = SetAbout AboutInfo

type UpdateLoginStatusMsg
  = FetchLogin (Result Http.Error UserInfo)
  | FetchLogout (Result Http.Error String)
  | ResumeExistingSession UserInfo

type UpdateErrorsMsg
  = AddError String
  | CloseError Int

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
  | StartInstance InstanceId
  | StopInstance InstanceId
  | AllInstancesExpanded (List InstanceId) Bool
  | EnterEditInstanceParameterValue Instance String String
  | SelectEditInstanceTemplate Instance (List Template) TemplateId
  | EnterNewInstanceParameterValue Template String String
  | ApplyParameterValueChanges Instance (Dict String String) TemplateId
  | DiscardParameterValueChanges Instance
  | ToggleEditInstanceSecretVisibility InstanceId String
  | ToggleNewInstanceSecretVisibility TemplateId String
  | ExpandNewInstanceForm Bool TemplateId
  | SubmitNewInstanceCreation TemplateId (Dict String String)
  | DiscardNewInstanceCreation TemplateId
  | DeleteSelectedInstances (Set String)

type UpdateTemplatesMsg
  = ListTemplates (List Template)
