module Utils.ParameterUtils exposing (getOtherParametersSorted, zipWithOrderIndex)

import Dict exposing (Dict)
import List
import Models.Resources.Template exposing (ParameterInfo)
import String
import Tuple


{-| Get other parameters, in order of the order-index and then alphabetical order.

"Other" parameters are all parameters except "id" which receives special treatment of being shown
first in all cases.

Note: We sort parameters by their ID not by their user-visible name, so the sort order in the UI
might seem different.

-}
getOtherParametersSorted : List ( Float, String ) -> List String
getOtherParametersSorted params =
    List.filter (\( orderIndex, param ) -> param /= "id") params
        |> List.sortBy (\( orderIndex, param ) -> ( orderIndex, String.toLower param ))
        |> List.map Tuple.second


{-| Zip the order index given from the parameterInfos with the parameters.

If no order index is given, we use Infinity such that those parameters go to the end.

-}
zipWithOrderIndex : Dict String ParameterInfo -> List String -> List ( Float, String )
zipWithOrderIndex parameterInfos parameters =
    parameters
        |> List.map
            (\p ->
                ( parameterInfos
                    |> Dict.get p
                    |> Maybe.andThen (\i -> i.orderIndex)
                    |> Maybe.withDefault (1 / 0)
                , p
                )
            )
