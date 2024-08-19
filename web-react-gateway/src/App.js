import React from 'react';
import { BrowserRouter as Router, Route, Switch, Redirect } from 'react-router-dom';
import Login from './components/Login';
import Table from './components/Table';
import Detail from './components/Detail';

function App() {
  const [isLoggedIn, setIsLoggedIn] = React.useState(false);

  return (
    <Router>
      <Switch>
        <Route exact path="/" render={() => (
          isLoggedIn ? <Redirect to="/table" /> : <Login setIsLoggedIn={setIsLoggedIn} />
        )} />
        <Route path="/table" component={Table} />
        <Route path="/detail/:id" component={Detail} />
      </Switch>
    </Router>
  );
}

export default App;