package uk.gov.hmcts.probate.util.diagrams.plantuml;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.probate.service.filebuilder.TextFileBuilderService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BuildStateDiagram {
    //use https://plantuml.com/
    private TextFileBuilderService textFileBuilderService = new TextFileBuilderService();

    private static final String CASE_TYPE_PREFIX = "CCD_Probate_";
    private static final String BASE_DIR = "ccdImports/configFiles/" + CASE_TYPE_PREFIX;
    private static final String CASE_TYPE_GRANT = "Backoffice";
    private static final String CASE_TYPE_CAVEAT = "Caveat";
    private static final String ROLE_CW = "caseworker-probate-issuer";
    private static final String ROLE_PP = "caseworker-probate-solicitor";
    private static final String COLOR_STATE = " #bfbfff";
    private static final String COLOR_STATE_READONLY = " #f2f2ff";
    private static final String COLOR_EVENT = " #ff8080";
    private static final String COLOR_EVENT_READONLY = " #ffe6e6";
    private static final String ARROW = " --> ";
    private static final String ARROW_TERMINATOR = " --> ";
    private static final String CR = " \n";
    private static final String INFO_CRUD = "CRUD - ";

    private boolean filteredByRole = true;
    private String filteredByRoleName = ROLE_PP;
    private boolean showCallbacks = false;
    private boolean showCrud = false;

    public static void main(String[] args) throws IOException {
        new BuildStateDiagram().generateAll();
    }

    private void generateAll() throws IOException {
        generate(CASE_TYPE_GRANT);
        //generate("Caveat");
    }

    private void generate(String caseType) throws IOException {
        List<State> allStates = getAllStates(caseType);
        List<Event> allEvents = getAllEvents(caseType, allStates);

        String header = "@startuml" + CR;
        String keyState = "state STATE" + COLOR_STATE + CR;
        String keyStateReadonly = "state STATE_READONLY" + COLOR_STATE_READONLY + CR;
        String noteState = "note left of STATE : States" + CR;
        String keyEvent = "state EVENT" + COLOR_EVENT + CR;
        String keyEventReadonly = "state EVENT_READONLY" + COLOR_EVENT_READONLY + CR;
        String noteEvent = "note left of EVENT : Events" + CR;

        String footer = "@enduml";

        List<String> allRows = new ArrayList<>();
        allRows.add(header);
        allRows.add(keyState);
        allRows.add(keyStateReadonly);
        allRows.add(noteState);
        allRows.add(keyEvent);
        allRows.add(keyEventReadonly);
        allRows.add(noteEvent);
        allRows.add(CR);

        for (State state : allStates) {
            String id = state.getStateId();
            String stateName = separateWithCRs(state.getName());

            String stateRow = "state " + id + " as \"" + stateName + "\" " + getColorForState(state) + CR;
            allRows.add(stateRow);
            List<String> iRows = getAllInformationRows(state);
            allRows.addAll(iRows);

        }
        for (Event event : allEvents) {
            String id = event.getEventId();
            String eventName = separateWithCRs(event.getName());

            String eventRow = "state " + id + " as \"" + eventName + "\"" + getColorForEvent(event) + CR;
            allRows.add(eventRow);
            List<String> iRows = getAllInformationRows(event);
            allRows.addAll(iRows);
        }
        allRows.add(CR);
        allRows.add(CR);
        allRows.add(CR);
        for (Event event : allEvents) {
            String eventId = event.getEventId();
            if (event.getPre() == null && event.getPost() == null) {
                String preEventRow = "[*] " + ARROW_TERMINATOR + eventId + " \n";
                String postEventRow = eventId + ARROW_TERMINATOR + "[*] \n";

                allRows.add(preEventRow);
                allRows.add(postEventRow);
            } else if (event.getPre() == null && event.getPost() != null) {
                String preEventRow = "[*] " + ARROW_TERMINATOR + eventId + CR;
                allRows.add(preEventRow);

                String post = event.getPost().getStateId();
                String eventPostRow = eventId + ARROW + post + CR;
                allRows.add(eventPostRow);

            } else if (event.getPre() != null && event.getPost() != null) {
                String pre = event.getPre().getStateId();
                String post = event.getPost().getStateId();

                String preEventRow = pre + ARROW + eventId + CR;
                String eventPostRow = eventId + ARROW + post + CR;
                allRows.add(preEventRow);
                allRows.add(eventPostRow);

            }
            allRows.add(CR);
        }

        allRows.add(footer);
        textFileBuilderService.createFile(allRows, ",", CASE_TYPE_PREFIX + caseType
                + "_" + filteredByRoleName + "_state.txt");
        File source = new File("");
    }

    private List<String> getAllInformationRows(Event event) {
        List<String> all = new ArrayList<>();
        if (showCallbacks) {
            String start = addEventInfo(event, event.getStart(), "1 - ");
            if (start != null) {
                all.add(start);
            }
            String about = addEventInfo(event, event.getAbout(), "2 - ");
            if (about != null) {
                all.add(about);
            }
            String sub = addEventInfo(event, event.getSubmitted(), "3 - ");
            if (sub != null) {
                all.add(sub);
            }
        }
        if (showCrud) {
            String crud = addEventInfo(event, event.getCrud(), INFO_CRUD);
            if (crud != null && !"CRUD".equals(event.getCrud())) {
                all.add(crud);
            }
        }
        return all;
    }

    private List<String> getAllInformationRows(State state) {
        List<String> all = new ArrayList<>();
        if (showCrud) {
            String crud = addStateInfo(state, state.getCrud(), INFO_CRUD);
            if (crud != null && !"CRUD".equals(state.getCrud())) {
                all.add(crud);
            }
        }
        return all;
    }

    private String addEventInfo(Event event, String cb, String prefix) {
        return addInfo(event.getEventId(), cb, prefix);
    }

    private String addStateInfo(State state, String cb, String prefix) {
        return addInfo(state.getStateId(), cb, prefix);
    }

    private String addInfo(String id, String cb, String prefix) {
        return cb.isBlank() ? null : id + " : " + prefix + separateWithCRs(removeUri(cb)) + CR;
    }

    private String removeUri(String callback) {
        String toRemove = "http://\\$\\{CCD_DEF_CASE_SERVICE_BASE_URL\\}";
        return callback.replaceAll(toRemove, "");
    }

    private List<Event> getAllEvents(String caseType, List<State> allStates) throws IOException {
        String caseEvent = getStringFromFile(BASE_DIR + caseType + "/CaseEvent.json");
        Map<String, Object>[] caseEvents = new ObjectMapper().readValue(caseEvent, HashMap[].class);

        List<Event> allEvents = new ArrayList<>();
        for (Map<String, Object> eventMap : caseEvents) {
            String id = "";
            String name = "";
            String description = "";
            String pre = "";
            String post = "";
            String start = "";
            String about = "";
            String submitted = "";
            boolean showSummary = false;
            for (String key : eventMap.keySet()) {
                switch (key) {
                    case "ID":
                        id = eventMap.get(key).toString();
                        break;
                    case "Name":
                        name = eventMap.get(key).toString();
                        break;
                    case "Description":
                        description = eventMap.get(key).toString();
                        break;
                    case "PreConditionState(s)":
                        pre = eventMap.get(key).toString();
                        break;
                    case "PostConditionState":
                        post = eventMap.get(key).toString();
                        break;
                    case "CallBackURLAboutToStartEvent":
                        start = eventMap.get(key).toString();
                        break;
                    case "CallBackURLAboutToSubmitEvent":
                        about = eventMap.get(key).toString();
                        break;
                    case "CallBackURLSubmittedEvent":
                        submitted = eventMap.get(key).toString();
                        break;
                    case "ShowSummary":
                        showSummary = Boolean.valueOf(eventMap.get(key).toString());
                        break;
                }

            }

            State preDiagramState = getState(allStates, pre);
            State postDiagramState = getState(allStates, post);

            Event event = Event.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .pre(preDiagramState)
                    .post(postDiagramState)
                    .start(start)
                    .about(about)
                    .submitted(submitted)
                    .showSummary(showSummary)
                    .build();
            allEvents.add(event);
        }


        List<Event> allAuthdEvents = new ArrayList<>();
        String allAuthEvents = getStringFromFile(BASE_DIR + caseType + "/AuthorisationCaseEvent.json");
        Map<String, Object>[] authEvents = new ObjectMapper().readValue(allAuthEvents, HashMap[].class);
        for (Map<String, Object> authEvent : authEvents) {
            Event foundEvent = allEvents.stream().filter(e -> e.getId().equals(authEvent.get("CaseEventID"))).findFirst().get();
            String userRole = authEvent.get("UserRole").toString();
            if (!filteredByRole || filteredByRoleName.equals(userRole)) {
                foundEvent.setCrud(authEvent.get("CRUD").toString());
                allAuthdEvents.add(foundEvent);
            }
        }
        return allAuthdEvents;
    }

    private List<State> getAllStates(String caseType) throws IOException {
        String caseSates = getStringFromFile(BASE_DIR + caseType + "/State.json");
        Map<String, Object>[] allStatesMap = new ObjectMapper().readValue(caseSates, HashMap[].class);

        List<State> allStates = new ArrayList<>();
        for (Map<String, Object> statesMap : allStatesMap) {
            String id = "";
            String name = "";
            String description = "";
            for (String key : statesMap.keySet()) {
                switch (key) {
                    case "ID":
                        id = statesMap.get(key).toString();
                        break;
                    case "Name":
                        name = statesMap.get(key).toString();
                        break;
                    case "Description":
                        description = statesMap.get(key).toString();
                        break;
                }

            }

            State state = State.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .build();
            allStates.add(state);
        }


        List<State> allAuthdStates = new ArrayList<>();
        String allAuthSates = getStringFromFile(BASE_DIR + caseType + "/AuthorisationCaseState.json");
        Map<String, Object>[] authStates = new ObjectMapper().readValue(allAuthSates, HashMap[].class);
        for (Map<String, Object> state : authStates) {
            String userRole = state.get("UserRole").toString();
            if (!filteredByRole || filteredByRoleName.equals(userRole)) {
                State foundState = allStates.stream().filter(s -> s.getId().equals(state.get("CaseStateID"))).findFirst().get();
                foundState.setCrud(state.get("CRUD").toString());

                if (foundState != null) {
                    allAuthdStates.add(State.builder()
                            .id(foundState.getId())
                            .name(foundState.getName())
                            .description(foundState.getDescription())
                            .crud(foundState.getCrud())
                            .build());
                }
            }
        }

        return allAuthdStates;
    }

    private State getState(List<State> allDiagramStates, String find) {
        for (State State : allDiagramStates) {
            if (State.getId().equalsIgnoreCase(find)) {
                return State;
            }
        }
        return null;
    }

    public String getStringFromFile(String path) throws IOException {
        String fileAsString = "";
        try (InputStream in = new FileInputStream(path);
             BufferedReader r = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String str = null;
            StringBuilder sb = new StringBuilder(8192);
            while ((str = r.readLine()) != null) {
                sb.append(str);
            }
            fileAsString = sb.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return fileAsString;
    }

    private String separateWithCRs(String name) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (char c : name.toCharArray()) {
            if (c == " ".toCharArray()[0]) {
                builder.append("\\n");
                i = 0;
            } else if (i > 30) {
                builder.append("\\n");
                builder.append(c);
                i = 0;
            } else {
                builder.append(c);
            }
            i++;
        }
        return builder.toString();
    }

    private String getColorForState(State state) {
        String col= COLOR_STATE;
        if ("R".equals(state.getCrud())) {
            col = COLOR_STATE_READONLY;
        }

        return col;
    }

    private String getColorForEvent(Event event) {
        String col= COLOR_EVENT;
        if ("R".equals(event.getCrud())) {
            col = COLOR_EVENT_READONLY;
        }

        return col;
    }

}
