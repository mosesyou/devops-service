import React, { createContext, useContext, useMemo, useEffect } from 'react';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import useStore from './useStore';

const Store = createContext();

export function useCodeQualityStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')((props) => {
  const {
    children,
    intl: { formatMessage },
  } = props;

  const mainStore = useStore();

  useEffect(() => {
    mainStore.loadCodeQualityData();
  }, []);

  const value = {
    ...props,
    formatMessage,
    codeQuality: mainStore,
  };

  return (
    <Store.Provider value={value}>
      {children}
    </Store.Provider>
  );
}));
