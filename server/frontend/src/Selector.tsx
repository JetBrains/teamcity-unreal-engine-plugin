import { React } from '@jetbrains/teamcity-api';
import { Size } from "@jetbrains/ring-ui/components/input/input";
import TagsInput, { TagsInputRequestParams, ToggleTagParams } from "@jetbrains/ring-ui/components/tags-input/tags-input";
import { useRef, useState } from "react";
import { TagType } from '@jetbrains/ring-ui/components/tags-list/tags-list';
import { getTagsAutocompletion, isParameterSearch } from "./autocompletion";

type TagSelectorConfig = {
    readonly values: TagType[],
    readonly selectedValues: TagType[],
    readonly name: string,
    readonly separator: string,
    readonly allowAddNewTags: Boolean,
    readonly buildTypeId: string,
}

const TagSelector = (config: TagSelectorConfig) => {
    const [tags, setTags] = useState(config.selectedValues);
    const [selectorDisabled, setSelectorDisabled] = useState(false);
    const hiddenInput = useRef<HTMLInputElement>(null);
    const getValues = (selectedValues: TagType[]) => selectedValues.map(t => t.key).join(config.separator);

    const handleTagAdd = (params: ToggleTagParams) => {
        const tagAsParameterReference = () => {
            const referenceMarker = "%";
            return {
                key: referenceMarker + params.tag.key + referenceMarker,
                label: referenceMarker + params.tag.label + referenceMarker,
            };
        }

        const isPlainKnownValue = config.values.map((value) => value.key).includes(params.tag.key);
        const newTag = isPlainKnownValue ? params.tag : tagAsParameterReference();
        const updatedTags = [...tags, newTag];
        hiddenInput.current!.value = getValues(updatedTags);
        setTags(updatedTags);
    }

    const handleTagRemove = (params: ToggleTagParams) => {
        const updatedTags = tags.filter((t) => t !== params.tag);
        hiddenInput.current!.value = getValues(updatedTags);
        setTags(updatedTags);
    }

    // Since we're not controlling the runner's form, we need to somehow disable the selector on form submission.
    // TC automatically sets the "disabled" attribute for all inputs within the form.
    // Here, we just need to keep track of our hidden input and act accordingly.
    React.useEffect(() => {
        if (hiddenInput.current) {
            const observer = new MutationObserver((records) => {
                for (const mutation of records) {
                    if (mutation.attributeName === "disabled") {
                        setSelectorDisabled(hiddenInput.current?.disabled ?? false);
                    }
                }
            });
            observer.observe(hiddenInput.current, { attributes: true });
            return () => observer.disconnect();
        }
    })

    const dataSource = async (userInput: TagsInputRequestParams): Promise<readonly TagType[]> => {
        if (!isParameterSearch(userInput.query)) {
            return config.values;
        }
        return await getTagsAutocompletion(config.buildTypeId, userInput.query);
    };

    return (
        <>
            <TagsInput
                onAddTag={handleTagAdd}
                onRemoveTag={handleTagRemove}
                tags={tags}
                maxPopupHeight={300}
                dataSource={dataSource}
                allowAddNewTags={config.allowAddNewTags}
                placeholder={''}
                size={Size.L}
                disabled={selectorDisabled}
            />
            <input
                ref={hiddenInput}
                // "prop:" prefix is required so that the value is presented after form submission
                name={`prop:${config.name}`}
                type="hidden"
                value={getValues(tags)}
            />
        </>
      );
}

export {
    TagSelector,
    TagSelectorConfig,
}
