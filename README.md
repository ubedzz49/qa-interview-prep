# QA Interview Prep

Notes, automation guides and practice pages for a fresher preparing for a QA / test automation role.

**Site:** https://ubedzz49.github.io/qa-interview-prep/

## What is inside

| Folder | Content |
|---|---|
| `docs/notes/` | 5 notes: manual testing and SDLC, Selenium quick notes, API testing, Git and GitHub, how a tester thinks |
| `docs/guides/` | Selenium with Java and Playwright with Java: basics, all main methods, three complete automations |
| `docs/demo-pages/` | Three practice pages: password validation, full form, mini shop flow |
| `docs/pdf/` | PDF copy of every note and guide |
| `automation-code-java/` | Maven project with the three Selenium and three Playwright test classes |

`docs/` is published with GitHub Pages.

## Run the automation tests

Needs Java 11 or newer, Maven and Google Chrome.

```bash
cd automation-code-java
mvn test                                          # run everything
mvn test -Dtest=PasswordTest                      # Selenium password test
mvn test -Dtest="playwrighttests.PasswordTest"    # Playwright password test
mvn test -Dheadless=true                          # run without a browser window
```

Each test class has a `PAGE_URL` that points to the practice page on GitHub Pages, so only internet access is needed. To test a local copy, put the `file:///` path of a file from `docs/demo-pages/` in `PAGE_URL`.

## The three cases

1. **Password validation**: boundary values, equivalence partitions, negative cases, state transitions, hostile input.
2. **Full form**: every input type, dependent dropdowns, multi-select, file upload, alert and confirm popups.
3. **Complete flow**: login, search, cart, coupon, checkout, payment, logout.
