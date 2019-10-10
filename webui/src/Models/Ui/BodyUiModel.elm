module Models.Ui.BodyUiModel exposing (..)

import Array exposing (Array)
import Dict exposing (Dict)
import Models.Resources.Instance exposing (..)
import Models.Resources.NodeResources exposing (NodeResources)
import Models.Resources.Template exposing (..)
import Models.Ui.InstanceParameterForm exposing (..)
import Set exposing (Set)


type alias ResourceHoverMessage =
    { nodeName : String
    , resourceType : ResourceType
    , resourceSubType : ResourceSubType
    , resourceId : String -- resourceId can be "total" or the allocationId if it refers to an allocation
    , message : String
    , position : Float
    }


type ResourceType
    = CPU
    | Disk
    | Memory


type ResourceSubType
    = Host
    | Allocated
    | AllocatedUtilization


type alias TemporaryStates =
    { resourceHoverMessage : Maybe ResourceHoverMessage
    , expandedResourceAllocs : Set String
    }


initialTemporaryStates =
    { resourceHoverMessage = Nothing
    , expandedResourceAllocs = Set.empty
    }


type alias BodyUiModel =
    { expandedTemplates : Set TemplateId
    , selectedInstances : Set InstanceId
    , expandedInstances : Set InstanceId
    , instanceParameterForms : Dict InstanceId InstanceParameterForm
    , visibleEditInstanceSecrets : Set ( InstanceId, String )
    , visibleNewInstanceSecrets : Set ( TemplateId, String )
    , expandedNewInstanceForms : Dict TemplateId InstanceParameterForm
    , attemptedDeleteInstances : Maybe ( TemplateId, Set InstanceId )
    , temporaryStates : TemporaryStates
    }


initialModel =
    { expandedTemplates = Set.empty
    , selectedInstances = Set.empty
    , expandedInstances = Set.empty
    , instanceParameterForms = Dict.empty
    , visibleEditInstanceSecrets = Set.empty
    , visibleNewInstanceSecrets = Set.empty
    , expandedNewInstanceForms = Dict.empty
    , attemptedDeleteInstances = Nothing
    , temporaryStates = initialTemporaryStates
    }
