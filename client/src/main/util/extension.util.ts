import { Extendable, Extension } from "../model/Extension";
import _ from "lodash";

export const copiesExtensionId =
    "org.worldcubeassociation.tnoodle.SheetCopyCount";
export const colorSchemeExtensionId =
    "org.worldcubeassociation.tnoodle.ColorScheme";
export const frontendStatusExtensionId =
    "org.worldcubeassociation.tnoodle.CompetitionStatus";
export const fmcTranslationsExtensionId =
    "org.worldcubeassociation.tnoodle.FmcLanguages";
export const mbldCubesExtensionId =
    "org.worldcubeassociation.tnoodle.MultiScrambleCount";

export const getDefaultCopiesExtension = () => {
    return {
        id: copiesExtensionId,
        specUrl: "",
        data: {
            numCopies: 1,
        },
    };
};

export const buildFmcExtension = (selectedTranslations: string[]) => {
    return {
        id: fmcTranslationsExtensionId,
        specUrl: "",
        data: { languageTags: selectedTranslations },
    };
};

export const buildMbldExtension = (scramblesCount: number) => {
    return {
        id: mbldCubesExtensionId,
        specUrl: "",
        data: {
            requestedScrambles: scramblesCount,
        },
    };
};

export const findExtension = <T extends Extendable>(
    extendable: T,
    extensionId: string,
) => extendable.extensions.find((ext) => ext.id === extensionId);

export const removeExtension = <T extends Extendable>(
    extendable: T,
    extensionId: string,
) => {
    return {
        ...extendable,
        extensions: [
            ...extendable.extensions.filter((it) => it.id !== extensionId),
        ],
    };
};

export const upsertExtension = <T extends Extendable>(
    extendable: T,
    extension: Extension,
) => {
    const withoutExtension = removeExtension(extendable, extension.id);

    return {
        ...withoutExtension,
        extensions: [...withoutExtension.extensions, extension],
    };
};

export const setExtensionLazily = <T extends Extendable>(
    extendable: T,
    extensionId: string,
    buildExtension: () => Extension | null,
) => {
    const oldExtension = findExtension(extendable, extensionId);
    const newExtension = buildExtension();

    // null means that we should delete the extension.
    if (newExtension === null) {
        // is there is an extension that we _can_ delete in the first place?
        if (oldExtension !== undefined) {
            return removeExtension(extendable, extensionId);
        }
    } else {
        const extensionDataEqual = _.isEqual(
            oldExtension?.data,
            newExtension.data,
        );

        // did the extension data update?
        if (!extensionDataEqual) {
            return upsertExtension(extendable, newExtension);
        }
    }

    return extendable;
};
