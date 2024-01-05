export const getDefaultCompetitionName = () => {
    let date = new Date();
    return "Scrambles for " + date.toISOString().split("T")[0];
};

export const competitionName2Id = (competitionName: string) => {
    return competitionName.replace(/[\W]/gi, "");
};
