import { React, ReactDOM } from '@jetbrains/teamcity-api';
import { TagSelector, TagSelectorConfig } from './Selector';

export interface IUnrealRunner {
    renderTagSelector(elementId: string, config: TagSelectorConfig): void
}

const UnrealRunner: IUnrealRunner = {
  renderTagSelector(containerId: string, config: TagSelectorConfig): void {
    ReactDOM.render(
      <React.StrictMode>
          <TagSelector {...config}/>
      </React.StrictMode>,
      document.getElementById(containerId)
    );
  }
};

global.UnrealRunner = UnrealRunner;
