import {utils} from "@jetbrains/teamcity-api";

export type ParameterAutocompletionResponseItem = {
    value: string
    label: string
    meta: string
    selectable: boolean
}

export type ParameterAutocompletionResponse = ParameterAutocompletionResponseItem[]
export type GetParameterAutocompletionArg = { settingsId: string; term: string }

export const getParameterAutocompletion = async (args: GetParameterAutocompletionArg): Promise<ParameterAutocompletionResponse> => {
    const result = utils.requestJSON<ParameterAutocompletionResponse>(
        "admin/parameterAutocompletion.html?settingsId=" + args.settingsId + "&term=" + args.term, {
            method: 'GET', headers: {
                "Content-Type": "application/json"
            }
        });
    return await result
}
