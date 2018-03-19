import React from 'react';
import {mount} from 'enzyme';

import Table from './Table';

jest.mock('react-router-dom', () => {
  return {
    Link: ({children}) => {
      return <a>{children}</a>;
    }
  };
});

jest.mock('react-table', () => ({children, props}) => (
  <div>
    {children} {props}
  </div>
));

jest.mock('components', () => {
  return {
    Button: props => <button {...props}>{props.children}</button>
  };
});

it('should render without crashing', () => {
  mount(<Table {...{head: [], body: [], foot: []}} />);
});

it('shoud correctly format header', () => {
  const result = Table.formatColumns(['x', 'y', 'z']);

  expect(result).toEqual([
    {Header: 'x', accessor: 'x', minWidth: 40},
    {Header: 'y', accessor: 'y', minWidth: 40},
    {Header: 'z', accessor: 'z', minWidth: 40}
  ]);
});

it('shoud correctly format header', () => {
  const result = Table.formatData(['Header 1', 'Header 2', 'Header 3'], [['a', 'b', 'c']]);

  expect(result).toEqual([{header_1: 'a', header_2: 'b', header_3: 'c'}]);
});
