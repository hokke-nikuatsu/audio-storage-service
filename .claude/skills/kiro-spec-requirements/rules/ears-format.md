# EARS Format Guidelines

## Overview
EARS (Easy Approach to Requirements Syntax) is the standard format for acceptance criteria in spec-driven development.

EARS patterns describe the logical structure of a requirement (condition + subject + response) and are not tied to any particular natural language.  
All acceptance criteria are written in Japanese, the language this project uses for spec bodies (see AGENTS.md). <!-- project-override (see docs/adr/0002) -->
Express each EARS pattern with the fixed Japanese phrases below (`〜とき、`, `〜場合、`, `〜間、`, `〜構成では、`, `〜は常に`) instead of the English trigger keywords and `shall`. Do not mix English trigger keywords or `shall` into the sentence. End the response with a verb in the plain form (`〜する`). <!-- project-override (see docs/adr/0002) -->

## Primary EARS Patterns

### 1. Event-Driven Requirements
- **Pattern**: [event]とき、[system]は[response/action] <!-- project-override (see docs/adr/0002) -->
- **Use Case**: Responses to specific events or triggers
- **Example**: ユーザーが購入ボタンを押したとき、Checkout Service はカートの中身を検証する

### 2. State-Driven Requirements
- **Pattern**: [precondition]間、[system]は[response/action] <!-- project-override (see docs/adr/0002) -->
- **Use Case**: Behavior dependent on system state or preconditions
- **Example**: 決済を処理している間、Checkout Service は処理中の表示を出す

### 3. Unwanted Behavior Requirements
- **Pattern**: [trigger]場合、[system]は[response/action] <!-- project-override (see docs/adr/0002) -->
- **Use Case**: System response to errors, failures, or undesired situations
- **Example**: 正しくないクレジットカード番号が入力された場合、Web サイトはエラーメッセージを表示する

### 4. Optional Feature Requirements
- **Pattern**: [feature is included]構成では、[system]は[response/action] <!-- project-override (see docs/adr/0002) -->
- **Use Case**: Requirements for optional or conditional features
- **Example**: サンルーフを備えた構成では、車はサンルーフの操作パネルを持つ

### 5. Ubiquitous Requirements
- **Pattern**: [system]は常に[response/action] <!-- project-override (see docs/adr/0002) -->
- **Use Case**: Always-active requirements and fundamental system properties
- **Example**: 携帯電話は常に100グラム未満の重さである

## Combined Patterns
- [precondition]間に[event]とき、[system]は[response/action] <!-- project-override (see docs/adr/0002) -->
- [event]とき、[additional condition]であれば、[system]は[response/action] <!-- project-override (see docs/adr/0002) -->

## Subject Selection Guidelines
- **Software Projects**: Use concrete system/service name (e.g., "Checkout Service", "User Auth Module")
- **Process/Workflow**: Use responsible team/role (e.g., "Support Team", "Review Process")
- **Non-Software**: Use appropriate subject (e.g., "Marketing Campaign", "Documentation")

## Quality Criteria
- Requirements must be testable, verifiable, and describe a single behavior.
- Use objective language: state mandatory behavior in the plain form (`〜する`) and recommendations as `〜することが望ましい`; avoid ambiguous terms. <!-- project-override (see docs/adr/0002) -->
- Follow EARS syntax: [condition]、[system]は[response/action]. <!-- project-override (see docs/adr/0002) -->
