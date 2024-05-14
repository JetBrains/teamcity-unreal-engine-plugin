import { debounce } from "./debounce";
import {
    getParameterAutocompletion,
    GetParameterAutocompletionArg,
    ParameterAutocompletionResponse
} from "./api/parameters";
import { TagType } from "@jetbrains/ring-ui/components/tags-list/tags-list";

export const isParameterSearch = (term: string) => {
    const parameterReferenceMarker = "%";
    return term.startsWith(parameterReferenceMarker);
}

export async function getTagsAutocompletion(settingsId: string, term: string): Promise<TagType[]> {
    return convertToTags(await getAutocompletion(settingsId, term));
}

async function getAutocompletion(settingsId: string, term: string): Promise<ParameterAutocompletionResponse> {
    try {
        return await getParameterAutocompletionDebounced({
            settingsId: settingsId,
            term: trimReferenceMarker(term),
        });
    } catch (e) {
        console.error("Failed to get parameters autocompletion. Original error: " + JSON.stringify(e));
        return [];
    }
}

const getParameterAutocompletionDebounced = debounce((args: GetParameterAutocompletionArg) => getParameterAutocompletion(args), 300);

function convertToTags(parametersResponse: ParameterAutocompletionResponse): TagType[] {
    const parameterGroupNameIndicator = "--";
    const tags = parametersResponse.map(item => ({
        label: item.value,
        key: item.value,
        disabled: !item.selectable || item.value.includes(parameterGroupNameIndicator),
    }));
    const deduplicatedTags = new Map(tags.map(tag => [tag.key, tag])).values();
    return [...deduplicatedTags];
}

const trimReferenceMarker = (input: string) => {
    const parameterReferenceMarkerRegex = /%/g;
    return input.replace(parameterReferenceMarkerRegex, '');
}
